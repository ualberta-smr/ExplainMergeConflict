package data;

import static utils.StringUtils.INFO;
import static utils.StringUtils.delimiter;

import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import data.diff.DiffRequestGenerator;
import data.diff.MoreSidedDiffRequestGenerator;
import data.diff.ThreeSidedDiffRequestGenerator;
import data.diff.TwoSidedDiffRequestGenerator;
import gr.uom.java.xmi.diff.CodeRange;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.refactoringminer.api.RefactoringType;
import utils.StringUtils;

public class RefactoringInfo {

  private transient RefactoringEntry entry;
  private transient String groupId;
  private transient RefactoringType type;

  private DiffRequestGenerator requestGenerator = new TwoSidedDiffRequestGenerator();
  private String name;

  private String[][] uiStrings = new String[3][2];
  private String[] paths = new String[3];
  private ArrayList<String> moreSidedLeftPaths = new ArrayList<>();

  private Group group;

  private Set<String> includes = new HashSet<>();

  private boolean hidden = false;
  private boolean threeSided = false;
  private boolean moreSided = false;

  /**
   * Deserializes a RefactoringInfo.
   *
   * @param value string
   * @return the RefactoringInfo
   */
  public static RefactoringInfo fromString(String value) {
    String regex = delimiter(INFO, true);
    String[] tokens = value.split(regex, 15);
    return new RefactoringInfo()
        .setName(tokens[0])
        .setNameBefore(StringUtils.deSanitize(tokens[1]))
        .setNameAfter(StringUtils.deSanitize(tokens[2]))
        .setElementBefore(StringUtils.deSanitize(tokens[3]))
        .setElementAfter(StringUtils.deSanitize(tokens[4]))
        .setDetailsBefore(StringUtils.deSanitize(tokens[5]))
        .setDetailsAfter(StringUtils.deSanitize(tokens[6]))
        .setLeftPath(tokens[7])
        .setMidPath(tokens[8])
        .setRightPath(tokens[9])
        .setGroup(Group.valueOf(tokens[10]))
        .setThreeSided(tokens[11].equals("t"))
        .setHidden(tokens[12].equals("t"))
        .setRequestGenerator(tokens[11].equals("t")
            ? ThreeSidedDiffRequestGenerator.fromString(tokens[13])
            : TwoSidedDiffRequestGenerator.fromString(tokens[13]))
        .setIncludes(new HashSet<>(
            tokens[14].isEmpty() ? List.of() : Arrays.asList(tokens[14].split(regex))));
  }

  public SimpleDiffRequest generate(DiffContent[] contents) {
    return requestGenerator.generate(contents, this);
  }

  /**
   * Serializes a RefactoringInfo.
   *
   * @return string value
   */
  public String toString() {
    return String.join(delimiter(INFO),
        name,
        Stream.concat(
            Arrays.stream(uiStrings).flatMap(Arrays::stream),
            Arrays.stream(paths))
            .map(s -> s == null ? "" : s)
            .map(StringUtils::sanitize)
            .collect(Collectors.joining(delimiter(INFO))),
        group.toString(),
        threeSided ? "t" : "f",
        hidden ? "t" : "f",
        requestGenerator.toString(),
        String.join(delimiter(INFO), includes)
    );
  }

  public RefactoringInfo setRequestGenerator(DiffRequestGenerator requestGenerator) {
    this.requestGenerator = requestGenerator;
    return this;
  }

  public RefactoringInfo setIncludes(Set<String> includes) {
    this.includes = includes;
    return this;
  }

  /**
   * Adds this refactoring to the method history map.
   * Note that it should be called in chronological order.
   *
   * @param map for method history
   */
  public void addToHistory(Map<String, ArrayList<RefactoringInfo>> map) {
    changeKeys(map);
    String before = getNameBefore();
    String after = getNameAfter();
    if (group == Group.ATTRIBUTE) {
      before = getDetailsBefore() + "|" + getNameBefore();
      after = getDetailsAfter() + "|" + getNameAfter();
    }

    if (group != Group.VARIABLE) {
      ArrayList<RefactoringInfo> data = map.getOrDefault(before, new ArrayList<RefactoringInfo>());
      map.remove(before);
      ArrayList<RefactoringInfo> data2 = map.getOrDefault(after, new ArrayList<RefactoringInfo>());
      data.add(this);
      for (RefactoringInfo info : data) {
        if (!data2.contains(info)) {
          data2.add(info);
        }
      }
      map.put(after, data2);
    }
  }

  private void changeKeys(Map<String, ArrayList<RefactoringInfo>> map) {
    if ((group == Group.CLASS || group == Group.ABSTRACT || group == Group.INTERFACE)
        && !getNameBefore().equals(getNameAfter())) {
      changeAttributesSignature(map);
      changeMethodsSignature(map);
    }
  }

  private void changeMethodsSignature(Map<String, ArrayList<RefactoringInfo>> map) {
    map.keySet().stream()
        .filter(x -> !x.contains("|"))
        .filter(x -> x.contains(".")).filter(x -> x.substring(0, x.lastIndexOf("."))
        .equals(getNameBefore()))
        .forEach(signature -> {
          String methodName = signature.substring(signature.lastIndexOf(".") + 1);
          if (methodName.contains("(")) {
            methodName = methodName.substring(0, methodName.indexOf("("));
          }
          String newKey;
          //change constructor name in case of a class rename
          if (methodName.equals(getNameBefore().substring(getNameBefore().lastIndexOf(".") + 1))) {
            newKey =
                getNameAfter() + "." + getNameAfter().substring(getNameAfter().lastIndexOf(".") + 1)
                    + signature.substring(signature.indexOf("("));
          } else {
            newKey = getNameAfter() + signature.substring(signature.lastIndexOf("."));
          }
          map.put(newKey, map.getOrDefault(signature, new ArrayList<>()));
          map.remove(signature);
        });
  }

  private void changeAttributesSignature(Map<String, ArrayList<RefactoringInfo>> map) {
    map.keySet().stream()
        .filter(x -> x.contains("|")).filter(x -> x.substring(0, x.lastIndexOf("|"))
        .equals(getNameBefore()))
        .forEach(signature -> {
          String newKey = getNameAfter() + signature.substring(signature.lastIndexOf("|"));
          map.put(newKey, map.getOrDefault(signature, new ArrayList<>()));
          map.remove(signature);
        });
  }

  public RefactoringInfo addMarking(CodeRange left, CodeRange right, boolean hasColumns) {
    return addMarking(left, null, right, RefactoringLine.VisualisationType.TWO, null,
        RefactoringLine.MarkingOption.NONE, hasColumns);
  }

  public RefactoringInfo addMarking(CodeRange left, CodeRange right,
                                    Consumer<RefactoringLine> offsetFunction,
                                    RefactoringLine.MarkingOption option,
                                    boolean hasColumns) {
    return addMarking(left, null, right, RefactoringLine.VisualisationType.TWO, offsetFunction,
        option, hasColumns);
  }

  /**
   * Add line marking for diffwindow used to display refactorings.
   * Includes possibility for sub-highlighting
   */
  public RefactoringInfo addMarking(CodeRange left, CodeRange mid, CodeRange right,
                                    RefactoringLine.VisualisationType type,
                                    Consumer<RefactoringLine> offsetFunction,
                                    RefactoringLine.MarkingOption option,
                                    boolean hasColumns) {

    requestGenerator.addMarking(left, mid, right, type, offsetFunction, option, hasColumns);
    if (left != null) {
      setLeftPath(left.getFilePath());
      if (moreSided) {
        moreSidedLeftPaths.add(left.getFilePath());
      }
    }
    if (mid != null) {
      setMidPath(mid.getFilePath());
    }
    if (right != null) {
      setRightPath(right.getFilePath());
    }
    return this;
  }

  public void addAllMarkings(RefactoringInfo info) {
    requestGenerator.getMarkings().addAll(info.getLineMarkings());
  }

  public void addIncludedRefactoring(String refactoring) {
    this.includes.add(refactoring);
  }

  public Set<String> getIncludingRefactorings() {
    return includes;
  }

  public String getParent() {
    return entry.getParent();
  }

  public List<RefactoringLine> getLineMarkings() {
    return requestGenerator.getMarkings();
  }

  public long getTimestamp() {
    return entry.getTimeStamp();
  }

  public String getCommitId() {
    return entry.getCommitId();
  }

  public boolean isThreeSided() {
    return threeSided;
  }

  /**
   * Sets the refactoring info as three sided.
   *
   * @param threeSided boolean
   * @return this
   */
  public RefactoringInfo setThreeSided(boolean threeSided) {
    this.threeSided = threeSided;
    if (threeSided) {
      requestGenerator = new ThreeSidedDiffRequestGenerator();
    }
    return this;
  }

  public boolean isMoreSided() {
    return moreSided;
  }

  public RefactoringInfo setMoreSided(boolean moreSided) {
    this.moreSided = moreSided;
    if (moreSided) {
      requestGenerator = new MoreSidedDiffRequestGenerator();
    }
    return this;
  }

  public RefactoringEntry getEntry() {
    return entry;
  }

  public RefactoringInfo setEntry(RefactoringEntry entry) {
    this.entry = entry;
    return this;
  }

  public ArrayList<String> getMoreSidedLeftPaths() {
    return moreSidedLeftPaths;
  }

  public RefactoringType getType() {
    return type;
  }

  public RefactoringInfo setType(RefactoringType type) {
    this.type = type;
    return this;
  }

  public String getName() {
    return name;
  }

  public RefactoringInfo setName(String name) {
    this.name = name;
    return this;
  }

  public boolean isHidden() {
    return hidden;
  }

  public RefactoringInfo setHidden(boolean hidden) {
    this.hidden = hidden;
    return this;
  }

  public String getGroupId() {
    return groupId;
  }

  public RefactoringInfo setGroupId(String groupId) {
    this.groupId = groupId;
    return this;
  }

  public String getLeftPath() {
    return paths[0];
  }

  public RefactoringInfo setLeftPath(String leftPath) {
    paths[0] = leftPath;
    return this;
  }

  public String getMidPath() {
    return paths[1];
  }

  public RefactoringInfo setMidPath(String midPath) {
    paths[1] = midPath;
    return this;
  }

  public String getRightPath() {
    return paths[2];
  }

  public RefactoringInfo setRightPath(String rightPath) {
    paths[2] = rightPath;
    return this;
  }

  public String getNameBefore() {
    return uiStrings[0][0];
  }

  public RefactoringInfo setNameBefore(String nameBefore) {
    uiStrings[0][0] = nameBefore;
    return this;
  }

  public String getNameAfter() {
    return uiStrings[0][1];
  }

  public RefactoringInfo setNameAfter(String nameAfter) {
    uiStrings[0][1] = nameAfter;
    return this;
  }

  public String getElementBefore() {
    return uiStrings[1][0];
  }

  public RefactoringInfo setElementBefore(String elementBefore) {
    uiStrings[1][0] = elementBefore;
    return this;
  }

  public String getElementAfter() {
    return uiStrings[1][1];
  }

  public RefactoringInfo setElementAfter(String elementAfter) {
    uiStrings[1][1] = elementAfter;
    return this;
  }

  public String getDetailsBefore() {
    return uiStrings[2][0];
  }

  public RefactoringInfo setDetailsBefore(String detailsBefore) {
    uiStrings[2][0] = detailsBefore;
    return this;
  }

  public String getDetailsAfter() {
    return uiStrings[2][1];
  }

  public RefactoringInfo setDetailsAfter(String detailsAfter) {
    uiStrings[2][1] = detailsAfter;
    return this;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof RefactoringInfo)) {
      return false;
    }
    RefactoringInfo that = (RefactoringInfo) o;
    return Arrays.equals(uiStrings, that.uiStrings)
        && getName().equals(that.getName())
        && getType() == that.getType()
        && getGroup() == that.getGroup();
  }

  public Group getGroup() {
    return group;
  }

  public RefactoringInfo setGroup(Group group) {
    this.group = group;
    return this;
  }

  public void correctLines(String before, String mid, String after) {
    requestGenerator.correct(before, mid, after);
  }

  public void correctMoreSidedLines(List<String> befores, String after) {
    ((MoreSidedDiffRequestGenerator) requestGenerator).correct(befores, after);
  }

}

