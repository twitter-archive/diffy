    echo "Send some traffic to your Diffy instance"
    for i in {1..20}
    do
        sleep 0.1
        curl -s -i -H "Canonical-Resource : json" http://localhost:8880/json?ByteDance > /dev/null
        sleep 0.1
        curl -s -i -H "Canonical-Resource : json" http://localhost:8880/json?Twitter > /dev/null
        sleep 0.1
        curl -s -i -H "Canonical-Resource : json" http://localhost:8880/json?Airbnb > /dev/null
        sleep 0.1
        curl -s -i -H "Canonical-Resource : json" http://localhost:8880/json?Paytm > /dev/null
        sleep 0.1
        curl -s -i -H "Canonical-Resource : json" http://localhost:8880/json?Baidu > /dev/null
done
