#!/bin/bash 


v=$(git branch | grep '*')
regex="\* (.*)"

if [[ $v =~ $regex ]]
then
	branchName="${BASH_REMATCH[1]}"
	git push -f origin "$branchName"
else
	chrlen=${#v}
	echo "nemam $regex $v $chrlen"
fi