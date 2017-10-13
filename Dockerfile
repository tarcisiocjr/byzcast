FROM anapsix/alpine-java

COPY dist/ /root/

RUN chmod +x /root/startServerGlobal.sh
RUN chmod +x /root/startServerLocal.sh
RUN chmod +x /root/buildConfig.sh

CMD ["sh", "-c", "/root/buildConfig.sh && /root/startServerLocal.sh"]