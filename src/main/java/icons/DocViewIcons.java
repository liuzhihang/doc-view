package icons;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

/**
 * 图标管理
 *
 * @author liuzhihang
 * @date 2021/2/28 14:49
 */
public interface DocViewIcons {

    Icon DOC_VIEW = IconLoader.getIcon("/icons/doc.svg", DocViewIcons.class);
    Icon CLEAR = IconLoader.getIcon("/icons/clear.svg", DocViewIcons.class);
    Icon GET = IconLoader.getIcon("/icons/get.svg", DocViewIcons.class);
    Icon POST = IconLoader.getIcon("/icons/post.svg", DocViewIcons.class);
    Icon PUT = IconLoader.getIcon("/icons/put.svg", DocViewIcons.class);
    Icon DELETE = IconLoader.getIcon("/icons/delete.svg", DocViewIcons.class);
    Icon DUBBO = IconLoader.getIcon("/icons/dubbo.svg", DocViewIcons.class);
}
