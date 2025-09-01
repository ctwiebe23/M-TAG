doc:
	pandoc README.md -so doc/index.html -d pandoc.yml -d readme
	pandoc CHANGELOG.md -so doc/changelog/index.html -d pandoc.yml -d readme

build:
	lein uberjar

run +OPTIONS:
	lein run {{OPTIONS}}
