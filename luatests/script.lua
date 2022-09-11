function table.tostring(tbl, depth, n)
    n = n or 0; depth = depth or 5; if(not(tbl)) then return nil end
    if (depth == 0) then return string.rep(' ', n).."..."; end
    local output={}; local index=0; local size=0; for _ in pairs(tbl) do size=size+1; end
    for key, value in pairs(tbl) do index=index+1;
        key = string.format((type(key)=="string") and "[\"%s\"]" or "[%s]", key);
        if (type(value)=="table") then
            if (next(value)) then
                table.insert(output,string.rep(' ', n)..key.." = {");
                table.insert(output, table.tostring(value, depth - 1, n + 4));
                table.insert(output,string.rep(' ', n).."}"..((index==size) and "" or ","));
            else
                table.insert(output,string.rep(' ', n)..key.." = {}"..((index==size) and "" or ","));
            end
        else
            value=type(value) == "string" and string.format("\"%s\"", value) or tostring(value);
            table.insert(output,string.rep(' ', n)..key.." = "..value..((index==size) and "" or ","));
        end
    end
    return table.concat(output,"\n")
end
function table.print(tbl, depth, n) print(table.tostring(tbl, depth, n)) end

function tmp()
    package.path=package.path..";C:\\Games\\Kitsune\\app\\src\\main\\assets\\scripts\\?.lua"
    print(package.path)
    htmlparser=require("htmlparser")
    --local body=htmlparser.parse("<html lang=\"en\" data-color-mode=\"auto\" data-light-theme=\"light\" data-dark-theme=\"dark\" >\n  <head>\n  <link rel=\"dns-prefetch\" href=\"https://github.githubassets.com\">\n<link rel=\"dns-prefetch\" href=\"https://avatars.githubusercontent.com\">\n<link rel=\"dns-prefetch\" href=\"https://github-cloud.s3.amazonaws.com\">\n<link rel=\"dns-prefetch\" href=\"https://user-images.githubusercontent.com/\">\n<link rel=\"preconnect\" href=\"https://github.githubassets.com\" crossorigin>\n<link rel=\"preconnect\" href=\"https://avatars.githubusercontent.com\">\n</head>\n</html>",nil)

    tree = htmlparser.parse([[
		<n class="r t" a1 a2= a3='' a4="" a5='a"5"' a6="a'6'" a7='#.[] :()' a8='|*+-=?$^%&/' a9=a9 a10>
		    <h>1</h>
            <h>2</h>
            <h>3</h>
		</n>
	]])
    --table.print(tree("n.r.t"),3,0)
    print(tree("n")[1]:textonly())
    print(tree("n")[1].class)
    --print(tree("n:[a8]")[1]:attr("a6"))
    --print(attr(tree("n:[a8]")[1],"a6"))
    print(tree:select("n:[a8]"):attr(1,"a6"))
    --print(tree("[a1]"))
    print("hello")
end
--tmp()

--str="hello3.5 65.54 world"
--print(str:match("[0-9]*%.?[0-9]+"))
t={}
t[123]="kris"
t[1443]="ran"
table.print(t)
print(t[nil])
print(math.ceil(126/100))

