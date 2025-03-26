import org.apache.spark.sql._
import org.apache.spark.sql.functions._
import org.apache.spark.ml.feature._
import org.apache.spark.ml.classification.{RandomForestClassifier, RandomForestClassificationModel}
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.Pipeline

object Assignment2 {
  def main(args: Array[String]): Unit = {
    // Create Spark Session
    val spark = SparkSession.builder()
      .appName("TitanicMLAnalysis")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Load training data
    val trainDF = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("src/titanic_dataset/train.csv")

    // Load test data
    val testDF = spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv("src/titanic_dataset/test.csv")

    // Preprocessing function for training data
    def preprocessTrainData(df: DataFrame): DataFrame = {
      df.select(
        when(col("Sex") === "male", 1).otherwise(0).alias("Sex_Encoded"),
        col("Pclass"),
        when(col("Age").isNull, df.select(avg("Age")).first().getDouble(0))
          .otherwise(col("Age")).alias("Age_Imputed"),
        col("SibSp"),
        col("Parch"),
        when(col("Fare").isNull, df.select(avg("Fare")).first().getDouble(0))
          .otherwise(col("Fare")).alias("Fare_Imputed"),
        col("Survived").cast("double")
      )
    }

    // Preprocessing function for test data
    def preprocessTestData(df: DataFrame): DataFrame = {
      df.select(
        col("PassengerId"),
        when(col("Sex") === "male", 1).otherwise(0).alias("Sex_Encoded"),
        col("Pclass"),
        when(col("Age").isNull, df.select(avg("Age")).first().getDouble(0))
          .otherwise(col("Age")).alias("Age_Imputed"),
        col("SibSp"),
        col("Parch"),
        when(col("Fare").isNull, df.select(avg("Fare")).first().getDouble(0))
          .otherwise(col("Fare")).alias("Fare_Imputed")
      )
    }

    // Preprocess training data
    val processedTrainDF = preprocessTrainData(trainDF)

    // Feature vectorization for training
    val assembler = new VectorAssembler()
      .setInputCols(Array("Sex_Encoded", "Pclass", "Age_Imputed", "SibSp", "Parch", "Fare_Imputed"))
      .setOutputCol("features")

    // Random Forest Classifier
    val rf = new RandomForestClassifier()
      .setLabelCol("Survived")
      .setFeaturesCol("features")
      .setNumTrees(100)

    // Create Machine Learning Pipeline
    val pipeline = new Pipeline().setStages(Array(
      assembler,
      rf
    ))

    // Split training data
    val Array(trainingData, validationData) = processedTrainDF.randomSplit(Array(0.8, 0.2), seed = 42)

    // Train the model
    val model = pipeline.fit(trainingData)

    // Model Evaluation
    val predictions = model.transform(validationData)
    val evaluator = new BinaryClassificationEvaluator()
      .setLabelCol("Survived")
      .setRawPredictionCol("prediction")

    val accuracy = predictions
      .select(
        when(col("prediction") === col("Survived"), 1.0).otherwise(0.0).alias("correct")
      )
      .agg(avg("correct"))
      .first()
      .getDouble(0)

    println(s"Model Accuracy: ${accuracy * 100}%")

    // Preprocess test data
    val processedTestDF = preprocessTestData(testDF)

    // Predict test data
    val testPredictions = model.transform(processedTestDF)

    // Save predictions
    testPredictions.select("PassengerId", "prediction")
      .withColumnRenamed("prediction", "Survived")
      .write
      .mode("overwrite")
      .option("header", "true")
      .csv("src/titanic_dataset/predictions")

    // Stop Spark session
    spark.stop()
  }
}