
# latest alpine, others: jdk11, lts, slim, centos
# https://jenkins.io/download/lts/
# https://hub.docker.com/r/jenkins/jenkins/tags
# centos, because onepass binary?
FROM jenkins/jenkins:2.208-centos

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

ENV BITBUCKET_HOST bitbucket.imp.ac.at
# set plugins dir outsite home, which will be volume
# ENV JENKINS_OPTS

# this seems broken in combination with --volume from commandline
# VOLUME /var/jenkins_home/


ARG ONEPASS_VERSION=v0.8.0
ARG ONEPASS_SIGNKEY=3FEF9748469ADBE15DA7CA80AC2D62742012EA22
ADD https://cache.agilebits.com/dist/1P/op/pkg/${ONEPASS_VERSION}/op_linux_amd64_${ONEPASS_VERSION}.zip /tmp

USER root

RUN cd /tmp && unzip op_linux_amd64_${ONEPASS_VERSION}.zip && \
  gpg --recv-keys $ONEPASS_SIGNKEY && \
  gpg --verify op.sig op && \
  cp op /usr/local/bin/

USER jenkins
# groovy init scripts, must be in JENKINS_HOME
COPY init.groovy.d/ /usr/share/jenkins/ref/init.groovy.d/
# config file for bootstrapping credentials setup
COPY initial_credentials.yaml /usr/share/jenkins/ref/

# Jenkins config as code (JCasC) setup
ENV CASC_JENKINS_CONFIG /usr/share/jenkins/jenkins.conf.d/
COPY jenkins.conf.d/ /usr/share/jenkins/jenkins.conf.d/

# prepare ssh config
RUN umask 077 && mkdir -m 0700 /usr/share/jenkins/ref/.ssh && touch /usr/share/jenkins/ref/.ssh/known_hosts
# && ssh-keyscan -H ${BITBUCKET_HOST}:7999 >> /usr/share/jenkins/ref/.ssh/known_hosts
