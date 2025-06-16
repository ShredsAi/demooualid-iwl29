FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Add a non-root user to run our application
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Set up app directory and ownership
RUN mkdir -p /app/logs && \
    chown -R appuser:appgroup /app

# Copy the JAR file
COPY target/trip-request-matching-shred-1.0.0.jar /app/app.jar

# Set proper file ownership
RUN chown appuser:appgroup /app/app.jar

# Set non-root user
USER appuser

# Expose the application port
EXPOSE 8080

# Define JVM memory settings and other options
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Set Spring profile
ENV SPRING_PROFILES_ACTIVE="docker"

# Run the application
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar app.jar"]
