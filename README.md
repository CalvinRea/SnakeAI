I won first place at the https://snake.wits.ai/ competition. 

My snake is written exclusively in java and uses an improved A* search for pathfinding on a 2 by 2 grid. 
However, when my snake is trapped it used a specialised version of a BFS to find moves that allow the snake to survive the longest while trapped.
In addition there are many other heuristics such as going a certain distance towards the center when not closest to the apple.
Looking back on my code now I do see a few lines of messy or unnecessay code such as the PositionScore and PositionDistance class which could have just been a single class,
my logging class saves logs to my videos folder (why???) and so much more. 
Eventhough my algorithms remain efficient and my strategies sound, I should have put a bit more emphasis on readability for you.
