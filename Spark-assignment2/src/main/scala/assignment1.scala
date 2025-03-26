import org.apache.spark.sql._
import org.apache.spark.sql.functions._

object TitanicAnalysis2 {
  def main(args: Array[String]): Unit = {
    // Create Spark Session
    val spark = SparkSession.builder()
      .appName("TitanicDataAnalysis")
      .master("local[*]")
      .getOrCreate()

    // Read the CSV file
    val titanicDF = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("src/titanic_dataset/train.csv")

    //load testing data
    val testData = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("src/titanic_Dataset/test.csv")

    // 1. Average Ticket Fare by Ticket Class
    val avgFareByClass = titanicDF
      .groupBy("Pclass")
      .agg(avg("Fare").alias("Average_Fare"))
      .orderBy("Pclass")

    println("1. Average Ticket Fare by Class:")
    avgFareByClass.show()

    // 2. Survival Percentage by Ticket Class
    val survivalRateByClass = titanicDF
      .groupBy("Pclass")
      .agg(
        count("PassengerId").alias("Total_Passengers"),
        sum("Survived").alias("Survived_Passengers")
      )
      .withColumn("Survival_Rate",
        col("Survived_Passengers") / col("Total_Passengers") * 100)
      .orderBy("Pclass")

    println("\n2. Survival Rate by Class:")
    survivalRateByClass.show()

    // 3. Potential Rose DeWitt Bukater Passengers
    val roseCriteria = titanicDF
      .filter(
        col("Pclass") === 1 and  // First Class
          col("Sex") === "female" and  // Female
          col("Age") === 17 and  // Age 17
          col("Parch") === 1  // Traveling with parent
      )

    println("\n3. Potential Rose Passengers:")
    roseCriteria.show()

    // 4. Potential Jack Dawson Passengers
    val filledDF = titanicDF.withColumn("Age",
      when(col("Age").isNull, lit(19))
        .otherwise(col("Age"))
    )

    val jackCriteria = filledDF
      .filter(
        col("Pclass") === 3 and
          col("Sex") === "male" and
          (col("Age") === 19 or col("Age") === 20) and
          col("SibSp") === 0 and
          col("Parch") === 0
      )

    println("\n4. Potential Jack Passengers:")
    jackCriteria.show()

    // 5. Age Group Analysis
    val titanicWithAgeGroup = titanicDF
      .withColumn("AgeGroup",
        when(col("Age").between(1, 10), "1-10")
          .when(col("Age").between(11, 20), "11-20")
          .when(col("Age").between(21, 30), "21-30")
          .when(col("Age").between(31, 40), "31-40")
          .when(col("Age").between(41, 50), "41-50")
          .when(col("Age") > 50, "50+")
          .otherwise("Unknown")
      )

    val ageGroupSurvivalAnalysis = titanicWithAgeGroup
      .groupBy("AgeGroup")
      .agg(
        avg("Fare").alias("Average_Fare"),
        avg("Survived").alias("Survival_Rate"),
        count("PassengerId").alias("Total_Passengers")
      )
      .orderBy(desc("Survival_Rate"))

    println("\n5. Age Group Analysis (Survival Rate and Average Fare):")
    ageGroupSurvivalAnalysis.show()

    // Stop the Spark session
    spark.stop()
  }
}