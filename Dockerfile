
# latest alpine, others: jdk11, lts, slim
# https://jenkins.io/download/lts/
# https://hub.docker.com/r/jenkins/jenkins/tags
#FROM jenkins/jenkins:2.176.2-alpine
FROM jenkins/jenkins:2.189-alpine

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

ENV BITBUCKET_HOST bitbucket.imp.ac.at
# set plugins dir outsite home, which will be volume
# ENV JENKINS_OPTS

# this seems broken in combination with --volume from commandline
# VOLUME /var/jenkins_home/

ADD https://cache.agilebits.com/dist/1P/op/pkg/v0.5.7/op_linux_amd64_v0.5.7.zip /tmp

USER root
RUN unzip /tmp/op_linux_amd64_v0.5.7.zip op -d /usr/local/bin/

USER jenkins
# groovy init scripts, must be in JENKINS_HOME
COPY init.groovy.d/ /usr/share/jenkins/ref/init.groovy.d/

# Jenkins config as code (JCasC) setup
ENV CASC_JENKINS_CONFIG /usr/share/jenkins/jenkins.conf.d/
COPY jenkins.conf.d/ /usr/share/jenkins/jenkins.conf.d/

# prepare ssh config
RUN umask 077 && mkdir -m 0700 /usr/share/jenkins/ref/.ssh && touch /usr/share/jenkins/ref/.ssh/known_hosts
# && ssh-keyscan -H ${BITBUCKET_HOST}:7999 >> /usr/share/jenkins/ref/.ssh/known_hosts
