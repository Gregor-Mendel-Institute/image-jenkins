
# latest alpine, others: jdk11, lts, slim
FROM jenkins/jenkins:alpine

COPY plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN /usr/local/bin/install-plugins.sh < /usr/share/jenkins/ref/plugins.txt

ENV BITBUCKET_HOST bitbucket.imp.ac.at
ENV CASC_JENKINS_CONFIG /usr/share/jenkins/ref/jenkins.conf.d/


VOLUME /var/jenkins_home/
ADD https://cache.agilebits.com/dist/1P/op/pkg/v0.5.7/op_linux_amd64_v0.5.7.zip /tmp

USER root
RUN unzip /tmp/op_linux_amd64_v0.5.7.zip op -d /usr/local/bin/

USER jenkins
# groovy init scripts
COPY init.groovy.d/ /usr/share/jenkins/ref/init.groovy.d/
COPY jenkins.conf.d/ /usr/share/jenkins/ref/jenkins.conf.d/


# RUN ssh-keyscan -H ${BITBUCKET_HOST}:7999 >> ~/.ssh/known_hosts
