import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.WindowManager;
import com.intellij.openapi.wm.ex.StatusBarEx;

public class UIComponentsHelper {
    public static void setStatusBarText(Project project, String message) {
        if (project != null) {
            final StatusBarEx statusBar = (StatusBarEx) WindowManager.getInstance().getStatusBar(project);
            if (statusBar != null) {
                statusBar.setInfo(message);
            }
        }
    }
}