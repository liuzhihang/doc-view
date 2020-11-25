package com.liuzhihang.doc.view.service.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VirtualFile;
import com.liuzhihang.doc.view.service.ToolWindowService;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.util.*;

/**
 * DocViewDataService
 *
 * @author lvgorice@gmail.com
 * @version 1.0
 * @date 2020/11/23
 */
public class ToolWindowServiceImpl implements ToolWindowService {

    /**
     * 文件缓存，在刷新时清除
     */
    public static Map<String, VirtualFile> virtualFileCache = new HashMap<>();
    /**
     * 使用有序 map 保存临时项目文件树结构
     */
    private final Map<String, List<String>> docViewTreeTemp = new LinkedHashMap<>();

    /**
     * 加载 doc-view tree
     *
     * @param project 当前项目
     */
    @Override
    public void loadDocViewTree(Project project, JTree tree) {
        // doc-view 树模块
        DefaultTreeModel treeMode = (DefaultTreeModel) tree.getModel();
        DefaultMutableTreeNode modelRoot = (DefaultMutableTreeNode) treeMode.getRoot();
        // 清空当前树模块数据，重新加载
        modelRoot.removeAllChildren();
        docViewTreeTemp.clear();
        virtualFileCache.clear();

        // 根节点配置为项目名称
        DefaultMutableTreeNode treeRoot = new DefaultMutableTreeNode(project.getName());
        modelRoot.add(treeRoot);

        // 当前项目文件索引
        ProjectFileIndex fileIndex = ProjectFileIndex.SERVICE.getInstance(project);
        // 获取当前项目模块数
        Module[] modules = ModuleManager.getInstance(project).getModules();
        // 多模块项目
        if (modules.length > 1) {
            for (Module module : modules) {
                DefaultMutableTreeNode moduleRoot = new DefaultMutableTreeNode(module.getName());
                treeRoot.add(moduleRoot);

                fileIndex.iterateContentUnderDirectory(
                        Objects.requireNonNull(module.getModuleFile()).getParent(),
                        fileOrDir -> fileFilter(fileOrDir, fileOrDir.isDirectory()));
                loadTempTree(moduleRoot);
                // 释放内存
                docViewTreeTemp.clear();
            }
        } else {
            // 遍历项目目录
            fileIndex.iterateContent(fileOrDir -> fileFilter(fileOrDir, fileOrDir.isDirectory()));
            loadTempTree(treeRoot);
            // 释放内存
            docViewTreeTemp.clear();
        }

        // 更新树模块UI，重新加载
        tree.updateUI();
        treeMode.reload();
    }

    private void loadTempTree(DefaultMutableTreeNode moduleRoot) {
        for (String key : docViewTreeTemp.keySet()) {
            DefaultMutableTreeNode directory = new DefaultMutableTreeNode(key);
            docViewTreeTemp.get(key).forEach(value -> directory.add(new DefaultMutableTreeNode(value)));
            moduleRoot.add(directory);
        }
    }

    /**
     * 过滤文件
     *
     * @param fileOrDir     当前遍历节点
     * @param directoryFlag 是否为目录
     * @return 是否继续遍历
     */
    private boolean fileFilter(VirtualFile fileOrDir, boolean directoryFlag) {
        // 如果是文件
        if (!directoryFlag) {
            if (("java").equals(fileOrDir.getExtension())) {
                // 根据文件名过滤可疑的接口文件 ，暂时只保留包含 service 或 controller 文件
                String fileName = fileOrDir.getName().toLowerCase();
                // 服务类
                boolean service = fileName.contains("service");
                // 实现类
                boolean imp = fileName.contains("imp");
                boolean impl = fileName.contains("impl");
                // 控制类
                boolean action = fileName.contains("action");
                boolean controller = fileName.contains("controller");
                // TODO: 文件过滤规则可配置
                if ((service && !imp && !impl) || action || controller) {
                    // 使用 ‘/src/main/java/’ 后地址作为目录
                    String path = fileOrDir.getParent().getPath();
                    // File.separator 在 windows 环境取值有误，暂增加备用替换逻辑
                    path = path.replace(File.separator, ".")
                            .replace("/", ".")
                            .replace("\\", ".");
                    // TODO: 项目根目录可配置
                    String begin = "src.main.java.";
                    int beginIndex = path.indexOf(begin);
                    if (beginIndex == -1) {
                        begin = "src.";
                        beginIndex = path.indexOf(begin);
                    }
                    path = path.substring(beginIndex + begin.length());

                    List<String> fileNames = docViewTreeTemp.get(path);
                    fileNames = Optional.ofNullable(fileNames).orElse(new ArrayList<>());
                    fileNames.add(fileOrDir.getName());
                    docViewTreeTemp.put(path, fileNames);
                    virtualFileCache.put(path + fileOrDir.getName(), fileOrDir);
                }
            }
        } else {
            // 遍历到测试目录结束
            return !"test".equals(fileOrDir.getName()) || !fileOrDir.getName().contains("imp");
        }
        return true;
    }
}
