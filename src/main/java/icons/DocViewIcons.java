package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * 图标管理: 图标地址
 *
 * @author liuzhihang
 * @date 2021/2/28 14:49
 */
public interface DocViewIcons {

    /** Brand mark used by notifications and compact product references. */
    Icon DOC_VIEW = IconLoader.getIcon("/icons/docView.svg", DocViewIcons.class);

    /** Brand mark sized for the classic Tool Window stripe. */
    Icon DOC_VIEW_TOOL_WINDOW = IconLoader.getIcon("/icons/docViewToolWindow.svg", DocViewIcons.class);

    /** Brand mark sized for editor gutter markers. */
    Icon DOC_VIEW_GUTTER = IconLoader.getIcon("/icons/docViewGutter.svg", DocViewIcons.class);

}
