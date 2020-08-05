package refactoringml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import refactoringml.db.Database;
import refactoringml.db.HibernateConfig;
import static refactoringml.util.FilePathUtils.lastSlashDir;

public class RunSingleProject {
	private static final Logger log = LogManager.getLogger(RunSingleProject.class);

	public static void main(String[] args) throws Exception {
		// do we want to get data from the vars or not?
		// i.e., is this a local IDE test?
		boolean test = (args == null || args.length == 0);

		String gitUrl;
		String storagePath;
		String datasetName;
		String url;
		String user;
		String pwd;
		boolean storeFullSourceCode;

		if(test) {
			//TODO: remove references to mauricios desktop, e.g. make them arguments
			gitUrl = "/Users/mauricioaniche/Desktop/commons-lang";
			storagePath = "/Users/mauricioaniche/Desktop/results/";
			datasetName = "test";

			url = "jdbc:mysql://localhost:3306/refactoring2?useSSL=false";
			user = "root";
			pwd = "";
			storeFullSourceCode = true;

		} else {
			if (args == null || args.length != 7) {
				System.out.println("7 arguments: (dataset name) (git url or project directory) (output path) (database url) (database user) (database pwd) (true|false: store full source code?)");
				System.exit(-1);
			}

			datasetName = args[0].trim();
			gitUrl = args[1].trim();
			storagePath = lastSlashDir(args[2].trim());

			//TODO: is this extension necessary? it is inconsistent with the url handling in RunQueue
			url = args[3] + "?useSSL=false&useLegacyDatetimeCode=false&serverTimezone=UTC"; // our servers config.
			user = args[4];
			pwd = args[5];

			// store full analysed source code?
			storeFullSourceCode = Boolean.parseBoolean(args[6]);
			System.out.println("Store full source code? " + storeFullSourceCode);
		}

		Database db = null;
		try {
			db = new Database(new HibernateConfig().getSessionFactory(url, user, pwd));
		} catch(Exception e) {
			log.error("Error when connecting to the Database: ", e);
		}

		new App(datasetName, gitUrl, storagePath, db, storeFullSourceCode).run();
	}
}