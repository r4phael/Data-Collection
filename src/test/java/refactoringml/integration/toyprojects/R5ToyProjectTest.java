package refactoringml.integration.toyprojects;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import io.quarkus.test.junit.QuarkusTest;
import refactoringml.db.RefactoringCommit;
import refactoringml.db.StableCommit;
import refactoringml.integration.IntegrationBaseTest;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@QuarkusTest
public class R5ToyProjectTest extends IntegrationBaseTest {

	@Override
	protected String getRepo() {
		return "https://github.com/refactoring-ai-testing/toyrepo-r5.git";
	}

	// this test was to see whether the tool would collect classes with generics
	@Test
	public void refactorings() {

		List<RefactoringCommit> refactoringCommitList = getRefactoringCommits();
		Assertions.assertEquals(1, refactoringCommitList.size());

		assertRefactoring(refactoringCommitList, "31820e9d172ba571d93de14733101f8cb81853e8", "Extract Method", 1);
		Assertions.assertEquals("a.Test", refactoringCommitList.get(0).getClassName());

		for (RefactoringCommit refactoringCommit : refactoringCommitList){
			Assertions.assertFalse(refactoringCommit.classMetrics.isInnerClass);
		}
	}

	@Test
	public void stable() {
		// there are no instances of stable variables, as the repo is too small
		List<StableCommit> stableCommitList = getStableCommits();
		Assertions.assertEquals(0, stableCommitList.size());
	}

	@Test
	public void commitMetaData(){
		String commit = "31820e9d172ba571d93de14733101f8cb81853e8";
		assertMetaDataRefactoring(
				commit,
				"extract method",
				"Extract Method\tprivate print2() : void extracted from public print() : void in class a.Test",
				"@local//tmp/toyrepo-r5/" + commit,
				2,
				"3f56031b1be69cb8db1779ef79645c2ab93a1616");
	}

	@Test
	public void projectMetrics() {
		assertProjectMetrics(1, 0, 1, 19, 0, 19);
	}
}