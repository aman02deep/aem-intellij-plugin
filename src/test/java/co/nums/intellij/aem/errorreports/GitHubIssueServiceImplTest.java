package co.nums.intellij.aem.errorreports;

import co.nums.intellij.aem.test.junit.extensions.MockitoExtension;
import com.intellij.util.ReflectionUtil;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Issue;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.IssueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GitHubIssueServiceImplTest {

    private IssueService issueService;

    private GitHubIssueServiceImpl sut;

    @Captor
    private ArgumentCaptor<Map<String, String>> filterDataCaptor;

    @BeforeEach
    void setUp() {
        issueService = mock(IssueService.class);
        sut = new GitHubIssueServiceImpl();
        // Mockito is not able to inject it, so:
        ReflectionUtil.setField(GitHubIssueServiceImpl.class, sut, IssueService.class, "issueService", issueService);
    }

    @Test
    void shouldFilterByTitleFieldWhenFindingIssueByTitle() throws Exception {
        String testTitle = "Test title";
        when(issueService.getIssues(any(RepositoryId.class), anyMap())).thenReturn(emptyList());

        sut.findAutoGeneratedIssueByTitle(testTitle);

        verify(issueService).getIssues(any(RepositoryId.class), filterDataCaptor.capture());
        assertThat(filterDataCaptor.getValue())
                .contains(entry(IssueService.FIELD_TITLE, testTitle));
    }

    @Test
    void shouldFilterByAutoGeneratedLabelWhenFindingIssueByTitle() throws Exception {
        when(issueService.getIssues(any(RepositoryId.class), anyMap())).thenReturn(emptyList());

        sut.findAutoGeneratedIssueByTitle("any title");

        verify(issueService).getIssues(any(RepositoryId.class), filterDataCaptor.capture());
        assertThat(filterDataCaptor.getValue())
                .contains(entry(IssueService.FILTER_LABELS, GitHubIssueServiceKt.AUTO_GENERATED_LABEL_NAME));
    }

    @Test
    void shouldSortByNewestIssuesWhenFindingIssueByTitle() throws Exception {
        when(issueService.getIssues(any(RepositoryId.class), anyMap())).thenReturn(emptyList());

        sut.findAutoGeneratedIssueByTitle("any title");

        verify(issueService).getIssues(any(RepositoryId.class), filterDataCaptor.capture());
        assertThat(filterDataCaptor.getValue())
                .contains(
                        entry(IssueService.FIELD_SORT, IssueService.SORT_CREATED),
                        entry(IssueService.FIELD_DIRECTION, IssueService.DIRECTION_DESCENDING));
    }

    @Test
    void shouldReturnNullIssueWhenFindingIssueByTitleReturnedEmptyResults() throws Exception {
        when(issueService.getIssues(any(RepositoryId.class), anyMap())).thenReturn(emptyList());

        Issue issue = sut.findAutoGeneratedIssueByTitle("any title");

        assertThat(issue).isNull();
    }

    @Test
    void shouldReturnFirstIssueWhenFindingIssueByTitleReturnedMultipleIssues() throws Exception {
        Issue firstIssue = mock(Issue.class);
        when(issueService.getIssues(any(RepositoryId.class), anyMap())).thenReturn(asList(
                firstIssue,
                mock(Issue.class),
                mock(Issue.class)
        ));

        Issue issue = sut.findAutoGeneratedIssueByTitle("any title");

        assertThat(issue).isSameAs(firstIssue);
    }

    @Test
    void shouldCreateIssueInGitHub() throws Exception {
        when(issueService.createIssue(any(RepositoryId.class), any(Issue.class))).thenReturn(mock(Issue.class));
        Issue issueToSubmit = mock(Issue.class);

        sut.submitIssue(issueToSubmit);

        verify(issueService).createIssue(any(RepositoryId.class), eq(issueToSubmit));
    }

    @Test
    void shouldAddCommentInGitHub() throws Exception {
        when(issueService.createComment(any(RepositoryId.class), any(Integer.class), any(String.class)))
                .thenReturn(mock(Comment.class));

        sut.addComment(0, "Test comment");

        verify(issueService).createComment(any(RepositoryId.class), eq(0), eq("Test comment"));
    }

}
