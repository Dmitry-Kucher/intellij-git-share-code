import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.editor.impl.CaretImpl;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vcs.history.VcsRevisionNumber;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.StatusBarEx;
import git4idea.GitLocalBranch;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepoInfo;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import sun.rmi.runtime.Log;

import java.awt.datatransfer.StringSelection;
import java.util.Collection;
import java.util.List;

public class GitCopyReference extends AnAction {
    private Project project;
    private VirtualFile virtualFile;
    private Editor editor;
    private GitRepository repository;

    public void actionPerformed(AnActionEvent event) {
        this.project = event.getData(PlatformDataKeys.PROJECT);
        this.virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        this.editor = event.getData(PlatformDataKeys.EDITOR);

        if (this.isNullableException()) {
            return;
        }

        if (!GitUtil.isUnderGit(this.virtualFile)) {
            return;
        }

        GitRepositoryManager manager = GitUtil.getRepositoryManager(this.project);

        this.repository = manager.getRepositoryForFile(this.virtualFile);
        if (this.repository == null) {
            return;
        }

        String gitURL = this.getGitURL();
        String gitBranch = this.repository.getCurrentBranchName();

        List<CaretState> caretStates = this.editor.getCaretModel().getCaretsAndSelections();

        CaretState currentCaretState = caretStates.get(0);

        LogicalPosition startSelection = currentCaretState.getSelectionStart();
        LogicalPosition endSelection = currentCaretState.getSelectionEnd();
        if (startSelection == null || endSelection == null) {
            return;
        }

        Integer startLinePosition = startSelection.line;
        startLinePosition++;
        Integer endLinePosition = endSelection.line;
        endLinePosition++;

        String repositoryPath = this.repository.getRoot().getPath();
        String filePath = this.virtualFile.getPath();
        String relativePath = filePath.substring(repositoryPath.length());
        String toCopy = gitURL + "/blob/" + gitBranch + relativePath + "#L" + startLinePosition;
        if (!startLinePosition.equals(endLinePosition)) {
           toCopy = toCopy + "-" + endLinePosition;
        }
        CopyPasteManager.getInstance().setContents(new StringSelection(toCopy));
        UIComponentsHelper.setStatusBarText(this.project,  toCopy + " has been copied");
    }

    private String getGitURL () {
        String gitURL = "";
        for (GitRemote gitRemote : this.repository.getRemotes()) {
            String remoteName = gitRemote.getName();
            if (remoteName.equals("origin")) {
                gitURL = gitRemote.getFirstUrl();
                if(gitURL == null) {
                    return "";
                }
                if (gitURL.endsWith(".git")) {
                    gitURL = gitURL.substring(0, (gitURL.length() - 4));
                }

                if (gitURL.startsWith("git@")) {
                    String scheme = "http://";
                    gitURL = scheme + gitURL.substring(4);
                    Integer replaceStart = gitURL.lastIndexOf(":");
                    Integer replaceEnd = replaceStart + 1;
                    String replaceWith = "/";
                    StringBuilder gitURLBuilder = new StringBuilder(gitURL);
                    gitURLBuilder.replace(replaceStart, replaceEnd, replaceWith);
                    gitURL = gitURLBuilder.toString();
                }
                break;
            }
        }
        return gitURL;
    }

    private boolean isNullableException() {
        return (this.project == null || this.virtualFile == null || this.editor == null || this.project.isDisposed());
    }
}

