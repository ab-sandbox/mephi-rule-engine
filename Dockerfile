FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/rule-engine.jar .
COPY examples/ examples/

USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/rule-engine.jar"]

CMD ["examples/discount.rules", "examples/context.json"]
