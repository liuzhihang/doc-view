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

    /**
     * 插件图标：在侧栏树勇
     */
    Icon DOC_VIEW = IconLoader.getIcon("/icons/markdown.svg", DocViewIcons.class);

    /**
     * 清理缓存
     */
    Icon CLEAR = IconLoader.getIcon("/icons/clearCash.svg", DocViewIcons.class);

    /**
     * 上传
     */
    Icon UPLOAD = IconLoader.getIcon("/icons/upload.svg", DocViewIcons.class);

    /**
     * 下载
     */
    Icon DOWNLOAD = IconLoader.getIcon("/icons/download.svg", DocViewIcons.class);

    /**
     * pin
     */
    Icon PIN = IconLoader.getIcon("/icons/pin.svg", DocViewIcons.class);

    /**
     * editorPreview
     */
    Icon EDITOR_PREVIEW = IconLoader.getIcon("/icons/editorPreview.svg", DocViewIcons.class);

    /**
     * copy
     */
    Icon COPY = IconLoader.getIcon("/icons/copy.svg", DocViewIcons.class);

    /**
     * settings
     */
    Icon SETTINGS = IconLoader.getIcon("/icons/settings.svg", DocViewIcons.class);

    /**
     * checked
     */
    Icon CHECKED = IconLoader.getIcon("/icons/checked.svg", DocViewIcons.class);

    /**
     * json
     */
    Icon JSON = IconLoader.getIcon("/icons/json.svg", DocViewIcons.class);

    /**
     * refresh
     */
    Icon REFRESH = IconLoader.getIcon("/icons/refresh.svg", DocViewIcons.class);

    /**
     * export
     */
    Icon EXPORT = IconLoader.getIcon("/icons/export.svg", DocViewIcons.class);

    /**
     * expand ALL
     */
    Icon EXPAND_ALL = IconLoader.getIcon("/icons/expandAll.svg", DocViewIcons.class);

    /**
     * collapseAll
     */
    Icon COLLAPSE_ALL = IconLoader.getIcon("/icons/collapseAll.svg", DocViewIcons.class);

    /**
     * httpAPI
     */
    Icon HTTP_API = IconLoader.getIcon("/icons/httpAPI.svg", DocViewIcons.class);

}
