#!/bin/bash
# Reset
Color_Off='\033[0m'       # Text Reset

# Regular Colors
Black='\033[0;30m'        # Black
Red='\033[0;31m'          # Red
Green='\033[0;32m'        # Green
Yellow='\033[0;33m'       # Yellow
Blue='\033[0;34m'         # Blue
Purple='\033[0;35m'       # Purple
Cyan='\033[0;36m'         # Cyan
White='\033[0;37m'        # White

# Bold
BBlack='\033[1;30m'       # Black
BRed='\033[1;31m'         # Red
BGreen='\033[1;32m'       # Green
BYellow='\033[1;33m'      # Yellow
BBlue='\033[1;34m'        # Blue
BPurple='\033[1;35m'      # Purple
BCyan='\033[1;36m'        # Cyan
BWhite='\033[1;37m'       # White

# Underline
UBlack='\033[4;30m'       # Black
URed='\033[4;31m'         # Red
UGreen='\033[4;32m'       # Green
UYellow='\033[4;33m'      # Yellow
UBlue='\033[4;34m'        # Blue
UPurple='\033[4;35m'      # Purple
UCyan='\033[4;36m'        # Cyan
UWhite='\033[4;37m'       # White

# Background
On_Black='\033[40m'       # Black
On_Red='\033[41m'         # Red
On_Green='\033[42m'       # Green
On_Yellow='\033[43m'      # Yellow
On_Blue='\033[44m'        # Blue
On_Purple='\033[45m'      # Purple
On_Cyan='\033[46m'        # Cyan
On_White='\033[47m'       # White

# High Intensity
IBlack='\033[0;90m'       # Black
IRed='\033[0;91m'         # Red
IGreen='\033[0;92m'       # Green
IYellow='\033[0;93m'      # Yellow
IBlue='\033[0;94m'        # Blue
IPurple='\033[0;95m'      # Purple
ICyan='\033[0;96m'        # Cyan
IWhite='\033[0;97m'       # White

# Bold High Intensity
BIBlack='\033[1;90m'      # Black
BIRed='\033[1;91m'        # Red
BIGreen='\033[1;92m'      # Green
BIYellow='\033[1;93m'     # Yellow
BIBlue='\033[1;94m'       # Blue
BIPurple='\033[1;95m'     # Purple
BICyan='\033[1;96m'       # Cyan
BIWhite='\033[1;97m'      # White

# High Intensity backgrounds
On_IBlack='\033[0;100m'   # Black
On_IRed='\033[0;101m'     # Red
On_IGreen='\033[0;102m'   # Green
On_IYellow='\033[0;103m'  # Yellow
On_IBlue='\033[0;104m'    # Blue
On_IPurple='\033[0;105m'  # Purple
On_ICyan='\033[0;106m'    # Cyan
On_IWhite='\033[0;107m'   # White
if [ $# -ne 4 ]
then
   echo "usage: addNote.sh <serverUl> <BASEENTITYCODE> <message> <tag>"
   echo "e.g. ./addNote.sh http://localhost :8095 PER_USER1 'Hello everyone' phone "
   exit;
fi
mydate=`date -u +"%Y-%m-%dT%H:%M:%S.000Z"`
key=$2
message=$3
tag=$4
url=$1
echo ${mydate} ${url}  $key $message $tag
access_token=$(\
    curl -s -X POST https://keycloak.gada.io/auth/realms/internmatch/protocol/openid-connect/token \
    --user backend:6781baee-3b97-4b01-bcd4-b14aecd38fd8 \
    -H 'content-type: application/x-www-form-urlencoded' \
    -d 'username=user1&password=WelcomeToTheHub121!&grant_type=password' | jq --raw-output '.access_token' \
 )
#echo $KEYCLOAK_RESPONSE
#printf "${RED}Parsing access_token field, as we don't need the other elements:${NORMAL}\n"
TOKEN=`echo "$KEYCLOAK_RESPONSE" | jq -r '.access_token'`
TOKEN=$access_token
echo $TOKEN
echo $url
echo ""
CR=`curl -X POST "${url}/v7/notes"  --header "Authorization: Bearer $TOKEN" -H "accept: */*" -H "Content-Type: application/json"  --header 'Accept: application/json'  -d "{\"id\":0,\"content\":\"${message}\",\"created\":\"${mydate}\",\"sourceCode\":\"PER_USER1\",\"tags\":[{\"name\":\"${tag}\",\"value\":0}],\"targetCode\":\"${key}\",\"updated\":\"${mydate}\"}"`
#CR=`curl -X POST "https://internmatch-cyrus.gada.io/v7/notes"  --header "Authorization: Bearer $TOKEN" -H "accept: */*" -H "Content-Type: application/json"  --header 'Accept: application/json'  -d "{\"id\":0,\"content\":\"${message}\",\"created\":\"${mydate}\",\"sourceCode\":\"PER_USER1\",\"tags\":\"${tag}:0\",\"targetCode\":\"${key}\",\"updated\":\"${mydate}\"}"`
echo -e "${Green}${CR}${Color_Off}\n"
echo ""
echo ""

