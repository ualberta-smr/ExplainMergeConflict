package org.ualberta.smr.explainmergeconflict.ui;

import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.impl.ActionButton;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.Gray;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.OnePixelSplitter;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBLoadingPanel;
import com.intellij.ui.components.JBPanel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.components.BorderLayoutPanel;
import com.intellij.vcs.log.*;
import com.intellij.vcs.log.data.VcsLogData;
import com.intellij.vcs.log.impl.MainVcsLogUiProperties;
import com.intellij.vcs.log.impl.VcsLogContentUtil;
import com.intellij.vcs.log.impl.VcsLogManager;
import com.intellij.vcs.log.impl.VcsProjectLog;
import com.intellij.vcs.log.ui.MainVcsLogUi;
import com.intellij.vcs.log.ui.frame.MainFrame;
import com.intellij.vcs.log.ui.frame.VcsLogChangesBrowser;
import com.intellij.vcs.log.ui.highlighters.MyCommitsHighlighter;
import com.intellij.vcs.log.ui.highlighters.VcsLogHighlighterFactory;
import com.intellij.vcs.log.visible.filters.VcsLogFilterObject;
import icons.RefactorInsightIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.research.refactorinsight.data.RefactoringInfo;
import org.jetbrains.research.refactorinsight.services.RefactoringsBundle;
import org.jetbrains.research.refactorinsight.ui.tree.TreeUtils;
import org.jetbrains.research.refactorinsight.ui.tree.renderers.HistoryToolbarRenderer;
import org.jetbrains.research.refactorinsight.ui.windows.HistoryType;
import org.jetbrains.research.refactorinsight.utils.Utils;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TestWindow {
    private final VcsLogManager.VcsLogUiFactory<? extends MainVcsLogUi> factoryHead;
    private final VcsLogManager.VcsLogUiFactory<? extends MainVcsLogUi> factoryMergeHead;

    private MainVcsLogUi openLogTab1;
    private MainVcsLogUi openLogTab2;
    private ToolWindowManager toolWindowManager;
    private ToolWindow toolWindow;
    private Project project;
    private HistoryType type;

    /**
     * Constructor for the toolbar.
     *
     * @param project current project
     */
    public TestWindow(Project project, VcsLogUi logUi, VirtualFile file) {
        // com/intellij/vcs/log/ui/actions/OpenAnotherLogTabAction.java
        // VcsLogHighlighter
        // VcsCommitStyleFactory
        // com/intellij/vcs/log/history/FileHistoryUi.java
        this.project = project;
        System.out.println(logUi);
        toolWindowManager = ToolWindowManager.getInstance(project);
//        Utils.manager = toolWindowManager;
//        factory = VcsProjectLog.getInstance(project).getLogManager()
//                .getMainLogUiFactory("method history", VcsLogFilterObject.collection());

        // Apply branch filters
        VcsLogBranchFilter branchHeadFilter = VcsLogFilterObject.fromBranch("HEAD");

        // Apply file path filter
        Collection<VirtualFile> files = new ArrayList<>();
        files.add(file);
        VcsLogStructureFilter fileFilter = VcsLogFilterObject.fromVirtualFiles(files);
        VcsLogBranchFilter branchMergeHeadFilter = VcsLogFilterObject.fromBranch("testBranch1");

        // Apply all filters
        VcsLogFilterCollection headFilters = VcsLogFilterObject.collection(branchHeadFilter, fileFilter);
        VcsLogFilterCollection mergeHeadFilters = VcsLogFilterObject.collection(branchMergeHeadFilter, fileFilter);

        // Create log UIs - one for head and one for merge_head
        factoryHead = VcsProjectLog.getInstance(project).getLogManager()
        .getMainLogUiFactory("not history!", headFilters);
        factoryMergeHead = VcsProjectLog.getInstance(project).getLogManager()
            .getMainLogUiFactory("not history!", mergeHeadFilters);
        openLogTab1 = VcsProjectLog.getInstance(project).getLogManager().createLogUi("not history!", VcsLogManager.LogWindowKind.TOOL_WINDOW, true);
        VcsLogData data = VcsProjectLog.getInstance(project).getLogManager().getDataManager();
//        openLogTab1 = factoryHead.createLogUi(project, data);
        openLogTab2 = factoryMergeHead.createLogUi(project, data);

        System.out.println(openLogTab1.getProperties());
//        openLogTab1.isHighlighterEnabled()
        // TODO - this might be for applying one highlighter to one logui, instead of all?
        openLogTab1.getTable().addHighlighter(new TestHighlighter(data, openLogTab1));
        // TODO - approach 1: obtain list of all conflict region commits, then pass to highlighter. Use getTable().getModel()
        // TODO - approach 2: find the conflict regions in the highlighter instead. But code smell!
//        openLogTab1.getTable().getModel().
        System.out.println("Is highlighter enabled for openLogTab1?: " + openLogTab1.isHighlighterEnabled("not history!"));
//        MainVcsLogUiProperties.VcsLogHighlighterProperty.get("Bob");

        // Create components that we will use to apply to tool window
        JComponent mainComponent = openLogTab1.getMainComponent();
        JComponent secondComponent = openLogTab2.getMainComponent();
        mainComponent.setAutoscrolls(true);
        JBSplitter splitter = new JBSplitter(false);
        JBScrollPane pane = new JBScrollPane(mainComponent);
        splitter.setFirstComponent(pane);
        splitter.setSecondComponent(secondComponent);

        // Create tool window content
        Content content;
        ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
        content = contentFactory.createContent(splitter, "teststeststestes", false);

        // create tool window
        toolWindow =
                toolWindowManager.registerToolWindow("should not be history!",
                        true, ToolWindowAnchor.BOTTOM);
        toolWindow.getContentManager().addContent(content);
        toolWindow.getContentManager().setSelectedContent(content);
        toolWindow.show();
//        MainVcsLogUi testUi = VcsProjectLog.getInstance(project).openLogTab(logUi.getFilterUi().getFilters(), VcsLogManager.LogWindowKind.TOOL_WINDOW);
//        testUi.jumpToHash("6c1997c0");
    }

//    /**
//     * Display the toolbar.
//     *
//     * @param refactorings detected refactorings
//     * @param objectsName  name of the method
//     */
//    public void showToolbar(Set<RefactoringInfo> refactorings,
//                            String objectsName, DataContext datacontext, HistoryType type,
//                            @Nullable HashMap<String, Set<RefactoringInfo>> methodsHistory,
//                            @Nullable HashMap<String, Set<RefactoringInfo>> attributesHistory) {
//
//        this.type = type;
//        if (refactorings == null || refactorings.isEmpty()) {
//            showPopup(datacontext);
//        } else {
//            JBSplitter splitter = new JBSplitter(false, (float) 0.35);
//            List<RefactoringInfo> refactoringInfos = new ArrayList<>(refactorings);
//            Utils.chronologicalOrder(refactoringInfos);
//
//            Tree tree =
//                    createTree(refactoringInfos, methodsHistory, attributesHistory);
//            tree.setRootVisible(false);
//            //TreeUtils.expandAllNodes(tree, 0, tree.getRowCount());
//            tree.setCellRenderer(new HistoryToolbarRenderer());
//            addMouseListener(splitter, tree);
//            setFirstComponent(refactorings.size(), splitter, tree);
//            setSecondComponent(splitter);
//            showContent(objectsName, splitter);
//        }
//    }

    private void setSecondComponent(JBSplitter splitter) {
        final JBLabel component =
                new JBLabel(RefactoringsBundle.message("click.to.jump"), SwingConstants.CENTER);
        component.setForeground(Gray._105);
        splitter.setSecondComponent(component);
    }

    private void setFirstComponent(int size, JBSplitter splitter, Tree tree) {
        JBScrollPane pane = new JBScrollPane(tree);
        JBLabel label =
                new JBLabel(String.format(RefactoringsBundle.message("how.many.detected"),
                        size, size > 1 ? "s" : "", type.toString().toLowerCase()));
        label.setForeground(Gray._105);
        pane.setColumnHeaderView(label);
        splitter.setFirstComponent(pane);
    }

//    private void addMouseListener(JBSplitter splitter, Tree tree) {
//        tree.addMouseListener(new MouseAdapter() {
//            @Override
//            public void mouseClicked(MouseEvent e) {
//                if (e.getClickCount() == 2) {
//                    TreePath path = tree.getPathForLocation(e.getX(), e.getY());
//                    if (path == null) {
//                        return;
//                    }
//                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
//                            path.getLastPathComponent();
//                    if (node.isLeaf()) {
//                        RefactoringInfo info = HistoryToolbarRenderer.getRefactoringInfo(node);
//                        if (info == null) {
//                            return;
//                        }
//                        showLogTab(info, splitter);
//                    }
//                }
//            }
//        });
//    }

//    private void showLogTab(RefactoringInfo info, JBSplitter splitter) {
//        VcsLogData data = VcsProjectLog.getInstance(project).getLogManager().getDataManager();
//
//        openLogTab = factory.createLogUi(project, data);
//
//        Utils.add(openLogTab);
//        JComponent mainComponent = openLogTab.getMainComponent();
//        mainComponent.setAutoscrolls(true);
//        mainComponent.setSize(splitter.getSecondComponent().getSize());
//        splitter.setSecondComponent(mainComponent);
//        openLogTab.jumpToHash(info.getCommitId());
//
//        JBSplitter splitter1 = (JBSplitter) mainComponent.getComponent(0);
//
//        BorderLayoutPanel splitter2 = (BorderLayoutPanel) splitter1.getFirstComponent();
//
//        OnePixelSplitter panel = (OnePixelSplitter) splitter2.getComponent(1);
//        MainFrame mainFrame = (MainFrame) panel.getComponent(2);
//
//        OnePixelSplitter splitter3 = (OnePixelSplitter) mainFrame.getComponent(0);
//        OnePixelSplitter splitter4 = (OnePixelSplitter) splitter3.getComponent(2);
//        JBLoadingPanel panel1 = (JBLoadingPanel) splitter4.getComponent(1);
//        JComponent loadingDecorator = (JComponent) panel1.getComponent(0);
//        JPanel panel2 = (JPanel) loadingDecorator.getComponent(0);
//        VcsLogChangesBrowser browser = (VcsLogChangesBrowser) panel2.getComponent(0);
//        ((ActionButton) browser.getToolbar().getComponent().getComponent(1)).click();
//    }

//    @NotNull
//    private Tree createTree(List<RefactoringInfo> refactorings,
//                            HashMap<String, Set<RefactoringInfo>> methods,
//                            HashMap<String, Set<RefactoringInfo>> attributes) {
//
//        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
//        createRefactoringsTree(refactorings, root);
//
//        AtomicInteger expandable = new AtomicInteger();
//        root.breadthFirstEnumeration().asIterator().forEachRemaining((c) -> expandable
//                .getAndIncrement());
//
//        if (methods != null && !methods.isEmpty()) {
//            DefaultMutableTreeNode child = new DefaultMutableTreeNode(
//                    RefactoringsBundle.message("check.methods"));
//            addObjectsToTree(methods, child, true);
//            if (child.getChildCount() > 0) {
//                root.add(child);
//            }
//        }
//
//        if (attributes != null && !attributes.isEmpty()) {
//            DefaultMutableTreeNode child = new DefaultMutableTreeNode(
//                    RefactoringsBundle.message("check.fields"));
//            addObjectsToTree(attributes, child, false);
//            if (child.getChildCount() > 0) {
//                root.add(child);
//            }
//        }
//
//        Tree tree = new Tree(root);
//        for (int i = 0; i < expandable.get(); i++) {
//            tree.expandRow(i);
//        }
//        if (tree.getRowCount() > expandable.get()) {
//            int history1 = expandable.get();
//            tree.expandRow(history1);
//            DefaultMutableTreeNode n =
//                    (DefaultMutableTreeNode) tree.getPathForRow(history1).getLastPathComponent();
//            if (tree.getRowCount() > history1 + n.getChildCount() + 1) {
//                tree.expandRow(history1 + n.getChildCount() + 1);
//            }
//        }
//        return tree;
//    }

//    private void addObjectsToTree(HashMap<String, Set<RefactoringInfo>> objects,
//                                  DefaultMutableTreeNode child, boolean forMethods) {
//        objects.forEach((obj, refs) -> {
//            if (!refs.isEmpty()) {
//                DefaultMutableTreeNode m =
//                        new DefaultMutableTreeNode(forMethods
//                                ? obj.substring(obj.lastIndexOf(".") + 1)
//                                : obj.substring(obj.lastIndexOf("|") + 1));
//                List<RefactoringInfo> infos = new ArrayList<>(refs);
//                Utils.chronologicalOrder(infos);
//                createRefactoringsTree(infos, m);
//                child.add(m);
//            }
//        });
//    }

//    private void createRefactoringsTree(List<RefactoringInfo> refactorings,
//                                        DefaultMutableTreeNode root) {
//        for (RefactoringInfo ref : refactorings) {
//            TreeUtils.createHistoryTree(root, ref);
//        }
//    }

    private void showContent(String methodName, JComponent tree) {
        Content content;
        if ((content = toolWindow.getContentManager().findContent(methodName)) != null) {
            content.setComponent(tree);
        } else {
            ContentFactory contentFactory = ContentFactory.SERVICE.getInstance();
            content = contentFactory.createContent(tree, methodName, false);
            toolWindow.getContentManager().addContent(content);
        }

        toolWindow.getContentManager().setSelectedContent(content);
        toolWindow.setIcon(RefactorInsightIcons.toolWindow);
        toolWindow.show();
    }

//    private void showPopup(DataContext datacontext) {
//        JBPanel panel = new JBPanel(new GridLayout(0, 1));
//        panel.add(new JBLabel(RefactoringsBundle.message("no.ref.history")));
//        JBPopup popup = JBPopupFactory.getInstance()
//                .createComponentPopupBuilder(panel, null).createPopup();
//        popup.showInBestPositionFor(datacontext);
//    }
}
