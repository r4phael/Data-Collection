package refactoringml;

import com.rabbitmq.client.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.docker.DockerLookup;
import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import refactoringml.util.PropertiesUtils;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;
import static refactoringml.util.FilePathUtils.enforceUnixPaths;
import static refactoringml.util.FileUtils.*;
import static refactoringml.util.PropertiesUtils.getProperty;

public class RunQueue {
	private static final Logger log = LogManager.getLogger(RunQueue.class);
	//total count of queue failures during this run, e.g. a redelivery of a already delivered message or an empty message
	private static int queueFailures = 0;
	//name of the docker container if any exists, otherwise null
	private String containerName;
	//Store all failed projects of this worker in this file
	private File failedProjectsFile;

	public final static String QUEUE_NAME = "refactoring";
	private final Database db;
	private String storagePath;
	private String host;
	private final boolean storeFullSourceCode;

	public RunQueue(String host, String url, String user, String pwd, String storagePath, boolean storeFullSourceCode) {
		this.host = host;
		this.storagePath = storagePath;
		this.storeFullSourceCode = storeFullSourceCode;

		DockerLookup dockerLookup = new DockerLookup();
		containerName = enforceUnixPaths(dockerLookup.lookup(null, "containerName")).replace("/", "");
		if(containerName == null)
			containerName = "null";
		failedProjectsFile = new File(enforceUnixPaths(PropertiesUtils.getProperty("failedProjectsFile") + "_" + containerName));
		failedProjectsFile.getParentFile().mkdirs();

		db = new Database(new HibernateConfig().getSessionFactory(url, user, pwd));
		log.debug(toString());
	}

	public static void main(String[] args) throws Exception {
		// we gotta wait a few minutes before the queue is up and the db is up...
		Thread.sleep(1000 * Long.parseLong(getProperty("queueWaitTime")));

		String queueHost = System.getenv("QUEUE_HOST");
		String url = System.getenv("REF_URL");
		String user = System.getenv("REF_USER");
		String pwd = System.getenv("REF_DBPWD");
		boolean storeFullSourceCode = Boolean.parseBoolean(System.getenv("STORE_FILES"));
		String storagePath = System.getenv("STORAGE_PATH");

		cleanOldTempDir();
		new RunQueue(queueHost, url, user, pwd, storagePath, storeFullSourceCode).run();
	}

	private void run() throws IOException, TimeoutException, InterruptedException {
		ConnectionFactory factory = new ConnectionFactory();
		factory.setHost(host);

		while(true) {
			log.debug("Fetching a new element from the rabbitmq queue...");
			try (Connection connection = factory.newConnection();
				 Channel channel = connection.createChannel()) {

				GetResponse chResponse = channel.basicGet(QUEUE_NAME, true);
				processResponse(chResponse, channel);
			}
		}
	}

	private void processResponse(GetResponse chResponse, Channel channel) throws InterruptedException, IOException {
		if (chResponse != null && !chResponse.getEnvelope().isRedeliver()) {
			String message = new String(chResponse.getBody());
			log.debug("Got a new element from rabbitmq queue: " + message);
			processRepository(message);
		} else {
			handleQueueError(chResponse, channel);
		}
	}

	private void handleQueueError(GetResponse chResponse, Channel channel) throws InterruptedException, IOException {
		//exit the programme if the queue is empty, all projects were processed.
		AMQP.Queue.DeclareOk status = channel.queueDeclare(QUEUE_NAME, true, false, false, null);
		if(status != null && status.getMessageCount() == 0){
			shutdown(channel);
		}

		queueFailures++;
		if(queueFailures > 50)
			shutdown(channel);

		if(chResponse != null && chResponse.getEnvelope().isRedeliver()) {
			log.error("Got a redelivery from the queue: " + Arrays.toString(chResponse.getBody()));
		} else {
			log.error("Got an empty response from the queue: " + QUEUE_NAME + " and chResponse = " + chResponse);
		}
		Thread.sleep(1000 * queueFailures);
	}

	//properly shutdown the worker with the given exitcode
	//Only use for intentional shutdowns
	private void shutdown(Channel channel) throws IOException {
		//shutdown the connection with the rabbit queue
		if (channel != null && channel.isOpen())
			channel.getConnection().close();
		//shutdown the connection with the MYSQL database
		db.shutdown();
		//end the worker
		System.exit(0);
	}

	private void processRepository(String message) throws IOException {
		log.debug("Got a new element from rabbitmq queue: " + message);

		String[] msg = message.split(",");
		String dataset = msg[2];
		String gitUrl = msg[1];
		String projectInfo = gitUrl + ", " + dataset;
		appendToFile(failedProjectsFile, projectInfo + "\n");
		try {
			new App(dataset, gitUrl, storagePath, db, storeFullSourceCode).run();
		} catch (org.eclipse.jgit.api.errors.TransportException te){
			storeFailedProject(gitUrl, "Repository not available", te);
		} catch (Exception e) {
			log.fatal(e.getClass().getCanonicalName() + " while processing " + gitUrl, e);
			storeFailedProject(gitUrl, e.getClass().getCanonicalName(), e);
		}
		removeFromFile(failedProjectsFile, projectInfo);
	}

	private void storeFailedProject(String gitUrl, String failureReason, Exception exception) throws IOException {
		String failedProject = gitUrl + ", " + failureReason + ", " + exception.toString()  + "\n";
		appendToFile(failedProjectsFile, failedProject);
	}

	@Override
	public String toString(){
		return "RunQueue{\n"+
				"QUEUE_NAME = " + QUEUE_NAME + "\n" +
				"containerName=" + containerName + "\n" +
				"failedProjectsFile=" + failedProjectsFile.getAbsolutePath() + "\n" +
				"host = " + host + "\n" +
				"storagePath = " + storagePath + "\n" +
				"storeFullSourceCode = " + storeFullSourceCode +
				"db = " + db.toString() + "}";
	}
}
