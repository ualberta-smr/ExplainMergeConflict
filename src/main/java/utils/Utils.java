package utils;

import static org.refactoringminer.api.RefactoringType.CHANGE_ATTRIBUTE_TYPE;
import static org.refactoringminer.api.RefactoringType.CHANGE_PARAMETER_TYPE;
import static org.refactoringminer.api.RefactoringType.CHANGE_VARIABLE_TYPE;
import static org.refactoringminer.api.RefactoringType.RENAME_ATTRIBUTE;
import static org.refactoringminer.api.RefactoringType.RENAME_PARAMETER;


import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.LocalFilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.wm.ToolWindowManager;
import data.RefactoringInfo;
import git4idea.GitContentRevision;
import git4idea.GitRevisionNumber;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import org.refactoringminer.api.RefactoringType;

public class Utils {

  public static ToolWindowManager manager;
  /**
   * Used for storing and disposing the MainVcsLogs used for method history action.
   */
  private static ArrayList<Disposable> logs = new ArrayList<>();

  /**
   * Method used for disposing the logs that were created and shown for the method history action.
   * Called when the project is closing.
   * Avoids memory leaks.
   */
  public static void dispose() {
    for (Disposable log : logs) {
      Disposer.dispose(log);
    }
  }

  /**
   * Adds a Disposable object to the list.
   *
   * @param log to add.
   */
  public static void add(Disposable log) {
    logs.add(log);
  }

  /**
   * Sorts the info list for a better displaying.
   *
   * @param infos to be sorted.
   */
  public static void chronologicalOrder(List<RefactoringInfo> infos) {
    infos.sort(new Comparator<RefactoringInfo>() {
      @Override
      public int compare(RefactoringInfo o1, RefactoringInfo o2) {
        return Long.compare(o1.getEntry().getTimeStamp(), o2.getEntry().getTimeStamp());
      }
    });
  }

  /**
   * Finds the start and ending column of a word in a text.
   *
   * @param text Java Code
   * @param word Word to look for.
   * @param line In what line the word can be found.
   * @return Start and ending column in an int[]
   */
  public static int[] findColumns(String text, String word, int line) {
    String[] lines = text.split("\r\n|\r|\n");
    int startColumn = lines[line].indexOf(word) + 1;
    int endColumn = startColumn + word.length();
    return new int[] {startColumn, endColumn};
  }

  /**
   * Skips javadoc for a method or class.
   *
   * @param text to search in.
   * @param line current line.
   * @return the actual line.
   */
  public static int skipJavadoc(String text, int line) {
    String[] lines = text.split("\r\n|\r|\n");
    if (lines[line].contains("/**")) {
      for (int i = line + 1; i < lines.length; i++) {
        if (lines[i].contains("*/")) {
          return i + 1;
        }
      }
    }
    return line;
  }

  /**
   * Calculates offset.
   *
   * @param text   to search in
   * @param line   line
   * @param column column
   * @return offset
   */
  public static int getOffset(String text, int line, int column) {
    int offset = 0;
    String[] lines = text.split("\r\n|\r|\n");
    if (lines.length <= line - 2) {
      line = lines.length;
    }
    for (int i = 0; i < line - 1; i++) {
      offset += lines[i].length() + 1;
    }
    return offset + column - 1;
  }

  /**
   * Returns last line count.
   *
   * @param text to search in
   * @return length of the text
   */
  public static int getMaxLine(String text) {
    return text.split("\r\n|\r|\n").length;
  }

  /**
   * Gets the main refactoring in the list for combining purposes.
   *
   * @param infos list of refactorings
   * @return RefactoringInfo
   */
  public static RefactoringInfo getMainRefactoringInfo(List<RefactoringInfo> infos) {
    RefactoringInfo info = null;
    if (infos.stream().anyMatch(ofType(RENAME_ATTRIBUTE))
        && infos.stream().anyMatch(ofType(CHANGE_ATTRIBUTE_TYPE))) {
      info = infos.stream().filter(ofType(RENAME_ATTRIBUTE)).findFirst().get();
      info.setName("Rename and Change Attribute Type");
    } else if (infos.stream().anyMatch(ofType(RENAME_ATTRIBUTE))) {
      info = infos.stream().filter(ofType(RENAME_ATTRIBUTE)).findFirst().get();
      info.setName("Rename Attribute");
    } else if (infos.stream().anyMatch(ofType(CHANGE_ATTRIBUTE_TYPE))) {
      info = infos.stream().filter(ofType(CHANGE_ATTRIBUTE_TYPE)).findFirst().get();
      info.setName("Change Attribute Type");
    } else if (infos.stream().anyMatch(ofType(CHANGE_VARIABLE_TYPE))) {
      info = infos.stream().filter(ofType(CHANGE_VARIABLE_TYPE)).findFirst().get();
      info.setName("Rename and Change Variable Type");
    } else if (infos.stream().anyMatch(ofType(RENAME_PARAMETER))
        && infos.stream().anyMatch(ofType(CHANGE_PARAMETER_TYPE))) {
      info = infos.stream().filter(ofType(RENAME_PARAMETER)).findFirst().get();
      info.setName("Rename and Change Parameter Type");
    }
    return info;
  }

  private static Predicate<RefactoringInfo> ofType(RefactoringType type) {
    return (r) -> r.getType() == type;
  }

  /**
   * Checks and corrects the ranges returned by RefactoringMiner.
   *
   * @param info    refactoring info
   * @param project the open project
   * @return the corrected RefactoringInfo
   */
  public static RefactoringInfo check(RefactoringInfo info, Project project) {

    FilePath beforePath = new LocalFilePath(
        project.getBasePath() + "/"
            + info.getLeftPath(), false);
    FilePath midPath = !info.isThreeSided() ? null : new LocalFilePath(
        project.getBasePath() + "/"
            + info.getMidPath(), false);
    FilePath afterPath = new LocalFilePath(
        project.getBasePath() + "/"
            + info.getRightPath(), false);
    GitRevisionNumber afterNumber = new GitRevisionNumber(info.getCommitId());
    GitRevisionNumber beforeNumber = new GitRevisionNumber(info.getParent());


    try {
      String after = GitContentRevision
          .createRevision(afterPath, afterNumber, project).getContent();

      if (!info.isMoreSided()) {
        String before = GitContentRevision
            .createRevision(beforePath, beforeNumber, project).getContent();
        String mid = !info.isThreeSided() ? null : GitContentRevision
            .createRevision(midPath, afterNumber, project).getContent();

        info.correctLines(before, mid, after);
      } else {
        List<String> befores = new ArrayList<>();
        for (String path : info.getMoreSidedLeftPaths()) {
          FilePath filePath = new LocalFilePath(project.getBasePath() + "/" + path, false);
          befores.add(
              GitContentRevision.createRevision(filePath, beforeNumber, project).getContent());
        }
        info.correctMoreSidedLines(befores, after);
      }
    } catch (VcsException e) {
      System.out.println(info.getName() + " refactoring not implemented yet");
    }

    return info;
  }

  /**
   * Calculates the line of the package.
   *
   * @param text to search in.
   * @return line of the package.
   */
  public static int findPackageLine(String text) {
    String[] lines = text.split("\r\n|\r|\n");
    for (int i = 0; i < lines.length; i++) {
      if (lines[i].contains("package ")) {
        return i;
      } else if (lines[i].matches("^[a-zA-Z0-9]*$")) {
        return -1;
      }
    }
    return 0;
  }
}
