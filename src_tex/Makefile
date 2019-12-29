#Makfile for my latex
all : CR_ARA_MANET.pdf

#jobmindmap.pdf : jobmindmap.svg
	#rsvg-convert -f pdf -o $@ $<

CR_ARA_MANET.pdf : main.tex 
	latexmk -pdf -pdflatex="pdflatex -interaction=nonstopmode" -use-make $<

clean:
	latexmk -C
