FROM  mcr.microsoft.com/java/jre:8-zulu-debian10
ADD target/freshAirController-1.1-SNAPSHOT-jar-with-dependencies.jar ./

ENV user="user" \
    password="password" \
    freshAirSN="freshAirSN" \
    powerSN="powerSN" \
    powerLimit="30" \
    freshaiAliveDelay="60"

ADD start.sh ./
RUN sed -i 's/\r//' ./start.sh

CMD sh ./start.sh
