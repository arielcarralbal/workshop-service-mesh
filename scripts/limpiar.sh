#!/bin/bash

contentvs=`oc get virtualservice -n $PROJECT --as=system:admin 2>/dev/null` 

if [ -z "$contentvs" ]; then
    echo "No hay Virtual Services en el proyecto $PROJECT."
else
    contentvs=`awk 'NR>1' <<< "$contentvs"`

    names=`awk -v namespace="$PROJECT" '{ {print $1} }' <<< "$contentvs"`

    for name in "${names[@]}"
    do
        oc delete virtualservice "$name" -n "$PROJECT" --as=system:admin
    done
    
fi

contentdr=`oc get destinationrule -n "$PROJECT" --as=system:admin 2>/dev/null`

if [ -z "$contentdr" ]; then
    echo "No hay Destination Rule en el proyecto $PROJECT."
else
    contentdr=`awk 'NR>1' <<< "$contentdr"`

    names=`awk -v namespace="$PROJECT" '{ {print $1} }' <<< "$contentdr"`

    for name in "${names[@]}"
    do
        oc delete destinationrule "$name" -n "$PROJECT" --as=system:admin
    done
    
fi

contentse=`oc get serviceentry -n "$PROJECT" --as=system:admin 2>/dev/null`

if [ -z "$contentse" ]; then
    echo "No hay Service Entry en el proyecto $PROJECT."
else
    contentse=`awk 'NR>1' <<< "$contentse"`

    names=`awk -v namespace="$PROJECT" '{ {print $1} }' <<< "$contentse"`

    for name in "${names[@]}"
    do
        oc delete serviceentry "$name" -n "$PROJECT" --as=system:admin
    done
    
fi
