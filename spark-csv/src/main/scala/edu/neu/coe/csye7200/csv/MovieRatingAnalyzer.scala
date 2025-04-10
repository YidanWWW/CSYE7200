package edu.neu.coe.csye7200.csv

import org.apache.spark.sql.{DataFrame, SparkSession}
import org.apache.spark.sql.functions.{avg, stddev}
import scala.util.{Try, Success, Failure}

object MovieRatingAnalyzer extends App {

  // Create a Spark session in local mode.
  implicit val spark: SparkSession = SparkSession.builder()
    .appName("Rating Analysis")
    .master("local[1]")
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")

  val resourcePath: String = Option(getClass.getResource("/movie_metadata.csv"))
    .map(_.getPath)
    .getOrElse(throw new Exception("movie_metadata.csv not found in resources"))

  // Instantiate the analyzer with the CSV file path.
  val analyzer = MovieRatingAnalyzer(resourcePath)

  // Use a for-comprehension to chain operations and handle errors gracefully.
  val analysisResult: Try[(Double, Double)] = for {
    df      <- analyzer.readFile(spark)
    _       =  df.show(5)
    meanDF  <- analyzer.calcStats(df, "imdb_score", "mean")
    stdDF   <- analyzer.calcStats(df, "imdb_score", "std")
    mean    <- Try(meanDF.first().getDouble(0))
    std     <- Try(stdDF.first().getDouble(0))
  } yield (mean, std)

  analysisResult match {
    case Success((mean, std)) =>
      println(s"The mean rating is: $mean")
      println(s"The standard deviation of ratings is: $std")
    case Failure(ex) =>
      println(s"An error occurred: ${ex.getMessage}")
  }

  spark.stop()
}

case class MovieRatingAnalyzer(resource: String) {

  /**
   * Reads the CSV file using the provided SparkSession.
   * @param spark the SparkSession used to read the file.
   * @return a Try containing the DataFrame if successful, or a Failure if an error occurs.
   */
  def readFile(spark: SparkSession): Try[DataFrame] =
    Try(spark.read.option("header", true).csv(resource))

  /**
   * Calculates statistics for the specified column.
   * @param df the DataFrame containing the data.
   * @param col the column name (e.g., "imdb_score").
   * @param stats the type of statistic to calculate ("mean" or "std").
   * @return a Try containing the resulting DataFrame with the computed statistic.
   */
  def calcStats(df: DataFrame, col: String, stats: String): Try[DataFrame] = stats match {
    case "mean" => Try(df.select(avg(col)))
    case "std"  => Try(df.select(stddev(col)))
    case _      => Failure(new IllegalArgumentException("Only 'mean' and 'std' calculations are supported"))
  }
}
