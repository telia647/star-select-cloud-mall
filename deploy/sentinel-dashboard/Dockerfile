FROM eclipse-temurin:17-jre

ARG SENTINEL_VERSION=1.8.9
ARG SENTINEL_DASHBOARD_JAR_URL=https://gh.llkk.cc/https://github.com/alibaba/Sentinel/releases/download/1.8.9/sentinel-dashboard-1.8.9.jar

ADD ${SENTINEL_DASHBOARD_JAR_URL} /app/sentinel-dashboard.jar

ENV JAVA_OPTS="-Xms128m -Xmx256m"

EXPOSE 8858

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=8858 -Dcsp.sentinel.dashboard.server=localhost:8858 -Dproject.name=sentinel-dashboard -jar /app/sentinel-dashboard.jar"]
