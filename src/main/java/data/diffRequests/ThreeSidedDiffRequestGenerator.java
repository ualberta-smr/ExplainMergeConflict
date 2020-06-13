package data.diffRequests;

import static ui.windows.DiffWindow.REFACTORING_RANGES;

import com.intellij.diff.contents.DiffContent;
import com.intellij.diff.requests.SimpleDiffRequest;
import data.RefactoringInfo;
import data.RefactoringLine;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import utils.Utils;

public class ThreeSidedDiffRequestGenerator extends DiffRequestGenerator {

  private List<ThreeSidedRange> ranges;

  public ThreeSidedDiffRequestGenerator() {
  }

  @Override
  public SimpleDiffRequest generate(DiffContent[] contents, RefactoringInfo info) {
    SimpleDiffRequest request = new SimpleDiffRequest(info.getName(),
        contents[0], contents[1], contents[2],
        info.getLeftPath(), info.getMidPath(), info.getRightPath());
    request.putUserData(REFACTORING_RANGES, ranges);
    return request;
  }

  @Override
  public void correct(String before, String mid, String after) {
    super.correct(before, mid, after);
    ranges = lineMarkings.stream().map(RefactoringLine::getThreeSidedRange)
        .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return ranges.stream().map(ThreeSidedRange::toString)
        .collect(Collectors.joining(Utils.LIST_DELIMITER));
  }

  public static ThreeSidedDiffRequestGenerator fromString(String value){
    ThreeSidedDiffRequestGenerator generator = new ThreeSidedDiffRequestGenerator();
    generator.ranges = Arrays.stream(value.split(Utils.LIST_DELIMITER))
        .map(ThreeSidedRange::fromString).collect(Collectors.toList());
    return generator;
  }
}
