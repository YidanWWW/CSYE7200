package edu.neu.coe.csye7200.csv

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import scala.util.{Try, Success, Failure}

class MovieRatingTest extends AnyFlatSpec with Matchers {

  // Create a local SparkSession
  val spark: SparkSession = SparkSession
    .builder()
    .appName("Rating Analysis")
    .master("local[1]")
    .getOrCreate()

  // Set Spark logging level to ERROR
  spark.sparkContext.setLogLevel("ERROR")

  // Retrieve resource path for the CSV file from resources
  val resource = getClass.getResource("/movie_metadata.csv")
  val path = resource.getPath

  // Instantiate the analyzer with the file path
  val analyzer = MovieRatingAnalyzer(path)

  behavior of "readFile"
  it should "get movie_metadata.csv" in {
    val mdy: Try[DataFrame] = analyzer.readFile(spark)
    mdy.isSuccess shouldBe true
    mdy.foreach { d =>
      d.count() shouldBe 1609
      d.show(10)
    }
  }

  behavior of "calcStats"
  it should "work on small batch of data" in {
    // Define a small list of scores from the first 5 rows
    val small_list = List(7.9, 7.1, 6.8, 8.5, 7.1)
    import spark.implicits._

    analyzer.calcStats(small_list.toDF(), "value", "mean") match {
      case Success(df) =>
        df.first().getDouble(0) shouldBe 7.4799999999999995
      case Failure(e) =>
        fail(s"Expected success for 'mean', but got error: $e")
    }

    analyzer.calcStats(small_list.toDF(), "value", "std") match {
      case Success(df) =>
        df.first().getDouble(0) shouldBe 0.7014271166700075
      case Failure(e) =>
        fail(s"Expected success for 'std', but got error: $e")
    }
  }

  it should "work on the actual dataframe" in {
    val file: DataFrame = analyzer.readFile(spark) match {
      case Success(df) => df
      case Failure(e)  => fail(s"Error reading file: $e")
    }
    analyzer.calcStats(file, "imdb_score", "mean") match {
      case Success(df) =>
        df.first().getDouble(0) shouldBe 6.453200745804848
      case Failure(e) =>
        fail(s"Expected success for 'mean', but got error: $e")
    }
    analyzer.calcStats(file, "imdb_score", "std") match {
      case Success(df) =>
        df.first().getDouble(0) shouldBe 0.9988071293753289
      case Failure(e) =>
        fail(s"Expected success for 'std', but got error: $e")
    }
  }
}
