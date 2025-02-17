val h::t = List(1,2,3)
println(h)
println(t)

val x: Double = Math.PI
println(x)

//method definitions
def x = Math.PI
def x: Double = Math.PI/2 + 1

for(i <- List(1,2,3)) print(i)

for(i<- List(1,2,3)) yield i.toString

val xs = List(1,2,3)
for(x<-xs; y<-List('A', 'B', 'C')) yield(x,y)