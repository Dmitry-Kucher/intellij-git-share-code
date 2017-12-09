import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.*;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;
import org.jetbrains.annotations.NotNull;

import java.awt.datatransfer.StringSelection;
import java.util.*;

public class GitCopyReference extends AnAction {
    private Project project;
    private VirtualFile virtualFile;
    private Editor editor;
    private GitRepository repository;

    public void actionPerformed(AnActionEvent event) {
        try {
            this.initProjectRelatedParams(event);
        } catch (Exception e) {
            UIComponentsHelper.setStatusBarText(this.project, "an error has occurred");
        }

        String toCopy = this.getLinkToCopy();

        CopyPasteManager.getInstance().setContents(new StringSelection(toCopy));
        UIComponentsHelper.setStatusBarText(this.project,  toCopy + " has been copied");
    }

    private void initProjectRelatedParams(AnActionEvent event) throws Exception {
        this.project = event.getData(PlatformDataKeys.PROJECT);
        this.virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        this.editor = event.getData(PlatformDataKeys.EDITOR);

        if (this.isNullableException() || !GitUtil.isUnderGit(this.virtualFile)) {
            throw new Exception();
        }

        GitRepositoryManager manager = GitUtil.getRepositoryManager(this.project);

        this.repository = manager.getRepositoryForFile(this.virtualFile);
        if (this.repository == null) {
            throw new Exception();
        }
    }

    @NotNull
    private String getLinkToCopy() {
        String gitURL = this.getGitURL();
        String gitBranch = this.repository.getCurrentBranchName();
        String linePosition = this.getLinePositionSuffix();
        String relativePath = this.getRelativePath();
        return gitURL + "/blob/" + gitBranch + relativePath + linePosition;
    }

    private String getGitURL() {
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

    @NotNull
    private String getRelativePath() {
        String repositoryPath = this.repository.getRoot().getPath();
        String filePath = this.virtualFile.getPath();
        return filePath.substring(repositoryPath.length());
    }

    private String getLinePositionSuffix() {
        List<CaretState> caretStates = this.editor.getCaretModel().getCaretsAndSelections();

        CaretState currentCaretState = caretStates.get(0);

        LogicalPosition startSelection = currentCaretState.getSelectionStart();
        LogicalPosition endSelection = currentCaretState.getSelectionEnd();
        if (startSelection == null || endSelection == null) {
            return "";
        }

        Integer startLinePosition = startSelection.line;
        startLinePosition++;
        Integer endLinePosition = endSelection.line;
        endLinePosition++;

        String linePosition = "#L" + startLinePosition;
        if (!startLinePosition.equals(endLinePosition)) {
            linePosition += "-" + endLinePosition;
        }
        return linePosition;
    }

    private boolean isNullableException() {
        return (this.project == null || this.virtualFile == null || this.editor == null || this.project.isDisposed());
    }
}

