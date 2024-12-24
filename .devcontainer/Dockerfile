FROM mcr.microsoft.com/devcontainers/base:debian

RUN apt update
RUN apt install -y wget curl ant maven protobuf-compiler

# Install JDK 8
RUN wget https://javadl.oracle.com/webapps/download/GetFile/1.8.0_321-b07/df5ad55fdd604472a86a45a217032c7d/linux-i586/jdk-8u321-linux-x64.tar.gz \
&& tar xvzf jdk-8u321-linux-x64.tar.gz \
&& rm jdk-8u321-linux-x64.tar.gz \
&& mv jdk1.8.0_321/ /usr/lib/jvm \
&& update-alternatives --install /usr/bin/java java /usr/lib/jvm/jdk1.8.0_321/bin/java 9999 \
&& sudo update-alternatives --install /usr/bin/javac javac /usr/lib/jvm/jdk1.8.0_321/bin/javac 9999 \
&& sudo update-alternatives --install /usr/bin/jar jar /usr/lib/jvm/jdk1.8.0_321/bin/jar 9999

# Install C++ developer env
RUN dpkg --configure -a \
&& apt install -y cmake cmake-curses-gui libprotobuf-dev libgtest-dev libre2-dev \
libicu-dev libboost-dev libboost-thread-dev libboost-system-dev

# Install Appengine for demo
RUN echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list \
&& curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | gpg --dearmor -o /usr/share/keyrings/cloud.google.gpg \
&& apt update -y && apt install google-cloud-cli -y
RUN apt install -y python3

