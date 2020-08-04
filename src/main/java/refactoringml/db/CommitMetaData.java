package refactoringml.db;

import org.eclipse.jgit.revwalk.RevCommit;
import refactoringml.util.JGitUtils;

import javax.persistence.*;
import java.util.Calendar;
import java.util.GregorianCalendar;

@Entity
@Table(name = "CommitMetaData")
public class CommitMetaData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    //use the unique commit hash to relate from Yes and No to this one
    // for RefactoringCommit, this commit points to the commit the refactoring has happened
    // For No, this commit points to the first commit where the class was stable
    // (i.e., if a class has been to [1..50] commits before considered as instance
    // of no refactoring, commitId = commit 1.
    private String commitId;

    //original commit message
    @Lob
    private String commitMessage;
    //url to the commit on its remote repository, e.g. https://github.com/mauricioaniche/predicting-refactoring-ml/commit/36016e4023cb74cd1076dbd33e0d7a73a6a61993
    private String commitUrl;
    //Date this commit was made
    @Temporal(TemporalType.TIMESTAMP)
    private Calendar commitDate;

    //id of the parent commit, if none exists:
    // the parent commit points to the commit that we calculate the code metrics
    // (we calculate the metrics in the version of file *before* the refactoring)
    private String parentCommitId;

    @Deprecated // hibernate purposes
    public CommitMetaData() {this.commitId = "";}

    @Deprecated //testing purposes
    public CommitMetaData(String commitId, String fullMessage, String url, String parentId) {
        this.commitId = commitId.trim();
        this.commitDate = new GregorianCalendar();
        this.commitMessage = fullMessage.trim();
        this.commitUrl = url;
        this.parentCommitId = parentId.trim();
    }

    public CommitMetaData(RevCommit commit, Project project){
        this.commitId = commit.getName().trim();
        this.commitDate = JGitUtils.getGregorianCalendar(commit);
        this.commitMessage = commit.getFullMessage().trim();
        this.commitUrl = JGitUtils.generateCommitUrl(project.getGitUrl(), commitId, project.isLocal());
        this.parentCommitId = commit.getParentCount() == 0 ? "Null" : commit.getParent(0).getName().trim();
    }

    public String getCommitUrl (){return commitUrl;}

    public String getCommitId() {return commitId; }

    public String getCommitMessage (){return commitMessage;}

    public String getParentCommitId() {return parentCommitId; }

    public long getId() {
        return id;
    }

    @Override
    public String toString() {
        return "CommitMetaData{" +
                "commit=" + commitId +
                ", commitDate=" + commitDate +
                ", commitMessage=" + commitMessage +
                ", commitUrl=" + commitUrl +
                ", parentCommit='" + parentCommitId + '\'' +
                '}';
    }
}