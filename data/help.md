# 模拟JTT1078视频终端命令的方法
模拟JTT1078发送报文可以通过如下命令进行：
cat tcpdump.bin | pv -L 25k -q | nc 127.0.0.1 2935 