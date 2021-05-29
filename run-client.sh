SERVER_HOST="172.31.91.58"
SERVER_PORT="9090"
LOCATION="jars"
THREADS=1
CLEAN=0
DOC_SIZE=124
FILE_SUFF=1
REPEAT=$2

#echo "* COPY JARS TO $SERVER_HOST"
#scp ./jars/*.jar $SERVER_HOST:"$LOCATION/"

# echo "* START SERVER ON $SERVER_HOST"
#  (java -jar $LOCATION/WoCoServer.jar $SERVER_HOST $SERVER_PORT $CLEAN $THREADS > ./server.log ) &

# Clean up old logs
rm *.log


echo "* START CLIENTS"

for x in `seq 2 $1`
do
	 (java -jar ./jars/WoCoClient.jar $SERVER_HOST $SERVER_PORT $DOC_SIZE $REPEAT $FILE_SUFF > ./client$x.log) &
done
java -jar ./jars/WoCoClient.jar $SERVER_HOST $SERVER_PORT $DOC_SIZE $REPEAT $FILE_SUFF > ./client1.log

sleep 1

echo "* CLEANUP"
killall java

grep "Total " *.log | awk '{sum += $6} END {print sum}'
grep "Total " *.log | awk '{sum += $7} END {print sum}'
grep "Average " *.log | awk '{sum += $4} END {print sum}'
