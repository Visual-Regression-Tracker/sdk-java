package io.visual_regression_tracker.sdk_java;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class PathProviderTest {

  private final String apiUrl = "http://localhost:4200";
  private PathProvider pathProvider;

  @BeforeMethod()
  public void setUp() {
    pathProvider = new PathProvider(apiUrl);
  }

  @DataProvider(name = "shouldGetImageUrlCases")
  public Object[][] shouldGetImageUrlCases() {
    return new Object[][] {
      {null, null}, {"", null}, {"some", apiUrl.concat("/some")},
    };
  }

  @Test(dataProvider = "shouldGetImageUrlCases")
  public void shouldGetImageUrl(String imageName, String expectedResult) {
    String result = pathProvider.getImageUrl(imageName);

    assertThat(result, is(expectedResult));
  }
}
