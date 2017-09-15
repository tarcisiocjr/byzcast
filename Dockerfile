FROM ubuntu:16.04
RUN apt-get update -y
RUN apt-get install -y software-properties-common python-software-properties net-tools tree
RUN add-apt-repository ppa:webupd8team/java
RUN apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv EA312927
RUN echo "deb http://repo.mongodb.org/apt/ubuntu $(cat /etc/lsb-release | grep DISTRIB_CODENAME | cut -d= -f2)/mongodb-org/3.2 multiverse" | tee /etc/apt/sources.list.d/mongodb-org-3.2.list
RUN apt-get update -y
RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections
RUN echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections
RUN apt-get install -y oracle-java8-installer mongodb-org
RUN mkdir -p /data/db

COPY lib /root/lib

COPY bin /root/bftswarmLocal/bin
COPY config/local/ /root/bftswarmLocal/config
COPY script/local/ /root/bftswarmLocal
RUN chmod +x /root/bftswarmLocal/startServerLocal.sh

COPY bin /root/bftswarmGlobal/bin
COPY config/global/ /root/bftswarmGlobal/config
COPY config/local/ /root/bftswarmGlobal/config-local
COPY script/global/ /root/bftswarmGlobal
RUN chmod +x /root/bftswarmGlobal/startServerGlobal.sh

CMD ["sh", "-c", "/root/bftswarmGlobal/startServerGlobal.sh & /root/bftswarmLocal/startServerLocal.sh"]