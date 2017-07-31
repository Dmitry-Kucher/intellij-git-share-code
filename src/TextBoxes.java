import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.ide.CopyPasteManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.StatusBarEx;
import git4idea.GitLocalBranch;
import git4idea.GitUtil;
import git4idea.repo.GitRemote;
import git4idea.repo.GitRepoInfo;
import git4idea.repo.GitRepository;
import git4idea.repo.GitRepositoryManager;

import java.awt.datatransfer.StringSelection;
import java.util.Collection;
import java.util.List;

public class TextBoxes extends AnAction {
    // If you register the action from Java code, this constructor is used to set the menu item name
    // (optionally, you can specify the menu description and an icon to display next to the menu item).
    // You can omit this constructor when registering the action in the plugin.xml file.
    public TextBoxes() {
        // Set the menu item name.
        super("Text _Boxes");
        // Set the menu item name, description and icon.
        // super("Text _Boxes","Item description",IconLoader.getIcon("/Mypackage/icon.png"));
    }

    public void actionPerformed(AnActionEvent event) {
        Project project = event.getData(PlatformDataKeys.PROJECT);
        VirtualFile virtualFile = event.getData(PlatformDataKeys.VIRTUAL_FILE);
        Editor editor = event.getData(PlatformDataKeys.EDITOR);

        if (virtualFile == null || project == null || project.isDisposed()) {
            return;
        }

        if (!GitUtil.isUnderGit(virtualFile)) {
            return;
        }

        GitRepositoryManager manager = GitUtil.getRepositoryManager(project);
        List<GitRepository> repositories = manager.getRepositories();

        GitRepository repository = manager.getRepositoryForFile(virtualFile);
        if (repository == null) {
            return;
        }
        GitRepoInfo repoInfo = repository.getInfo();
        GitLocalBranch localBranch = repoInfo.getCurrentBranch();

        String gitURL = "";
        for (GitRemote gitRemote : repository.getRemotes()) {
            String remoteName = gitRemote.getName();
            if (remoteName.equals("origin")) {
                gitURL = gitRemote.getFirstUrl();
                if(gitURL == null) {
                    return;
                }
                if (gitURL.endsWith(".git")) {
                    gitURL = gitURL.substring(0, (gitURL.length() - 4));
                    System.out.println(gitURL);
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
            }
        }

        String toCopy = gitURL;
        CopyPasteManager.getInstance().setContents(new StringSelection(toCopy));
        setStatusBarText(project,  toCopy + " has been copied");

//        String txt= Messages.showInputDialog(project, "What is your name?", "Input your name", Messages.getQuestionIcon());
//        Messages.showMessageDialog(project, "Hello, " + txt + "!\n I am glad to see you.", "Information", Messages.getInformationIcon());
    }

    private static void setStatusBarText(Project project, String message) {
        if (project != null) {
            final StatusBarEx statusBar = (StatusBarEx) WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(message);
            }
        }
    }
}

