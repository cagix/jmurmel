#
# Dockerfile for JMurmel w/o graphics
#
# Will build a docker image from a local clone of https://github.com/mayerrobert/jmurmel.git
# Needs docker or podman and a local git repo containing the jmurmel files.
# A local Java- or maven installation is not needed.
#
# Do something like
#
#    $ git clone https://github.com/mayerrobert/jmurmel.git
#    $ cd jmurmel
#
# and then:
#
# Usage:
#
#    $ podman build -t jmurmel .
#    $ podman run -it --rm jmurmel
#
# The "podman build..." command will build a docker image from the local jmurmel git repo.
# The "podman run..." command will launch an interactive REPL session.
#
# Optional: after the build command some docker images could be deleted
# unless you want to keep them around to rebuild again and/ or use them for other purposes:
#
#    $ podman rmi maven:3.8.5-openjdk-18-slim
#
# maybe followed by
#
#    $ podman image prune
#

FROM maven:3.8.5-openjdk-18-slim AS builder

WORKDIR jmurmel
COPY . .

# "jlink ... --strip-debug" fails because oraclelinux:8-slim is missing objcopy. Would save 4MB in the final image.
# Could add "jlink ... --add-modules java.desktop" which would add AWT and Swing, but that would only make sense 

RUN mvn -B package -pl lambda -DskipTests && \
    jlink --output jdkbuild/jdk --compress=2 --no-header-files --no-man-pages --add-modules java.base,java.desktop,jdk.compiler,jdk.zipfs,jdk.jfr,jdk.localedata,java.management


FROM oraclelinux:8-slim

WORKDIR jmurmel
COPY --from=builder /jmurmel/jdkbuild /jmurmel/lambda/target/jmurmel.jar /jmurmel/samples.mlib/mlib.lisp ./

RUN microdnf -y --nodocs install libX11 libXext libXrender libXtst freetype && microdnf -y clean all

# this will probably not work and you will need to specify the X-server on the commandline e.g.
# $ podman -it --rm --env DISPLAY=12.34.56.78:0.0
ENV DISPLAY=localhost:0.0

ENTRYPOINT [ "./jdk/bin/java", "-Dsun.java2d.opengl=true", "-jar", "jmurmel.jar" ]
