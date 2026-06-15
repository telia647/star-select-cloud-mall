FROM eclipse-temurin:17-jre

ARG SENTINEL_VERSION=1.8.9

ADD https://github.com/alibaba/Sentinel/releases/download/${SENTINEL_VERSION}/sentinel-dashboard-${SENTINEL_VERSION}.jar /app/sentinel-dashboard.jar

ENV JAVA_OPTS="-Xms128m -Xmx256m"

EXPOSE 8858

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=8858 -Dcsp.sentinel.dashboard.server=localhost:8858 -Dproject.name=sentinel-dashboard -jar /app/sentinel-dashboard.jar"]
