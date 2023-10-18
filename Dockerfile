# Use an official OpenJDK image as the base image
FROM openjdk:11-jdk

# Set the working directory
WORKDIR /target

ARG JAR_FILE=target/*.jar
# Copy the JAR file from the build output to the image
COPY target/school-managment-0.0.1-SNAPSHOT.jar app.jar

# Expose the port that the application will run on
EXPOSE 8085

# Start the application
CMD ["java", "-jar", "app.jar"]



#docker build -t car-rental-latest .


