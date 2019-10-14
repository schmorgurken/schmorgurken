# schmorgurken

Schmorgurken is a [Gherkin language](https://en.wikipedia.org/wiki/Cucumber_(software)#Gherkin_.28Language.29)
based [Behaviour Driven Development](https://en.wikipedia.org/wiki/Behavior-driven_development) 
testing tool for Clojure that is broadly compatible with 
[Cucumber](https://github.com/cucumber/cucumber).

Tests are written in a plain language format that is accessible to both the developer and user
and forms a common ground for establishing and validating the behaviour of a system.

## Installation

[![Clojars Project](https://img.shields.io/clojars/v/schmorgurken.svg)](https://clojars.org/schmorgurken)

To install Schmorgurken, just add the following to your `project.clj` dependencies

```clojure
[schmorgurken.schmorgurken "0.1.2"]
```

### Requirements

Schmorgurken is built and tested with Clojure 1.10 and JDK 8 although it should work with 
earlier versions.  

### Clojurescript support

Currently there is no support for Clojurescript although this may be added in a future
release.


## Getting started

This is not intended to be a full tutorial to learn Gherkin, so only the essentials
are covered here.

### Running 

Schmorgurken embraces the standard core Clojure testing library (clojure.test) and hence tests 
can be run using the standard **lein test** command.

### Features
A *feature file* contains a single *Feature* element which *must* be the first 
syntactic element in the feature file.  The keyword *Feature:* may be followed by a 
short description which can continue over several lines.  The descriptive text is ignored.
 
```gherkin
Feature: FX Trading feature
         Open Position - In order to open a position
         As a trader
         I want to send a trade order
```

### Scenarios
A *scenario* describes a functional test case. The keyword *Scenario:* may be followed 
by a short description. A feature may have multiple scenarios. Each scenario contains 
one or more *steps*.

```gherkin
Scenario: Market Order
    Given that my position in EURUSD is 0 at 1.34700
    And the market for EURUSD is at [1.34662;1.34714]
    When I submit an order to BUY 1000000 EURUSD at MKT
    Then a trade should be made at 1.34714
    And my position should show LONG 1000000 EURUSD at 1.34714
```

### Steps
Steps are defined by one of the keywords - *Given*, *When*, *Then* or *And*.  
Schmorgurken considers them all equivalent, but the scenario should use the keywords
appropriately to describe the setting up (*Given*), running the testing scenario (*When*)
and the validation (*Then*). 

### Connecting the steps to the code

Having defined a behavioural specification, the feature is connected to the code to be 
tested by a *step definition handler* (sometimes shortened to *"stepdef"*).

```clojure
(ns schmorgurken.full-test
  (:require [clojure.test :refer :all])
  (:use (schmorgurken core)))
```

In order to create the best readability it is recommended to "use" the library
rather than "require" it.

There are five functions that are used in writing step definition handlers to link the
feature file to the code to be tested - *Feature, Given, When, Then,* and *And* which should
correspond to the usage in the feature file.

The *Feature* function takes the parameters - 

* A location from which to load the feature file.  Schmorgurken loads feature files from the classpath and 
may refer to either one specific file or a directory.  If a directory is specified 
then all files a loaded recursively from that point.
* One or more step definitions specified by the functions *Given, When, Then,* or *And*

Each of the step definitions takes a regular expression (regex) to match and the function 
that should then be called given the match.  Parameters extracted from the match are passed
to the function, so the arity of the function must match the number of capture groups
in the regex, plus one extra to pass through the state.

### Regex as glue
Regular expressions are used to match text elements in the feature file to link to the stepdef handlers
in the test code.  Parameters in the step definitions can be extracted by match groups in the regex
definition.

### State

Each step handler function *must* have an extra parameter (which is always the first) which
will hold the return value from the previous function.  This allows step handler functions to 
avoid having to store global state in an atom between each step handler call-back.
 
### Example

This example will match the Market Order scenario specified earlier - 

```clojure
(Feature "features/trade.feature"
         (Given #"^that my position in (.*) is 0 at (.*)$" 
                (fn [_ currency-pair rate] ... ))
         
         (And #"^the market for (.*) is at \[(.*);(.*)\]$" 
              (fn [return-from-given currency-pair buy sell] ... ))
         
         (When #"^I submit an order to (.*) (.*) (.*) at (.*)$", 
               (fn [return-from-and dirn qty currency-pair type] ... ))
         
         (Then #"^a trade should be made at (.*)$", 
               (fn [return-from-when price] ... ))
         
         (And #"^my position should show (.*) (.*) (.*) at (.*)$", 
              (fn [return-from-then pos-dirn qty currency-pair price] ...)))

```

### Scenario Outlines
In order to avoid extensive repetition in the feature file, scenario outlines enable
specification of multiple examples using the same scenario steps.
A table of data defines each of the examples to be tested in the step definitions.  Again, a 
*Scenario Outline:* may have an optional description.

A *Scenario Outline:* is followed by the step definitions and finally by a table of 
data which indicated by the keyword *Examples:*.

```gherkin
Feature: Feed planning
  Scenario Outline: feeding a suckler cow
    Given the cow weighs <weight> kg
    When we calculate the feeding requirements
    Then the energy should be <energy> MJ
    And the protein should be <protein> kg
    Examples:
      | weight | energy | protein |
      | 450    | 26500  | 215     |
      | 500    | 29500  | 245     |
      | 575    | 31500  | 255     |
      | 600    | 37000  | 305     |
```

This example is semantically equivalent to -

```gherkin
Feature: Feed planning
  Scenario: feeding a suckler cow
    Given the cow weighs 450 kg
    When we calculate the feeding requirements
    Then the energy should be 26500 MJ
    And the protein should be 215 kg

Scenario: feeding a suckler cow
    Given the cow weighs 500 kg
    When we calculate the feeding requirements
    Then the energy should be 29500 MJ
    And the protein should be 245 kg

Scenario: feeding a suckler cow
    Given the cow weighs 575 kg
    When we calculate the feeding requirements
    Then the energy should be 31500 MJ
    And the protein should be 255 kg

Scenario: feeding a suckler cow
    Given the cow weighs 600 kg
    When we calculate the feeding requirements
    Then the energy should be 37000 MJ
    And the protein should be 305 kg
```

### Tables
Tables in Schmorgurken are pipe-character delimited.  As the table is parsed, the 
elements are stripped of leading and trailing spaces.

The first row is the title row and defines the parameter names.

Multiple data rows follow the title row.  Data rows may also contain 'Special' 
characters may be escaped with a backslash.  Supported characters are - 

<table>
<tr><th>Escape</th><th>Character</th></tr>
<tr><td>\|</td><td>Pipe character</td></tr>
<tr><td>\n</td><td>New line</td></tr>
<tr><td>\t</td><td>Tab</td></tr>
<tr><td>\b</td><td>Backspace</td></tr>
<tr><td>\r</td><td>Return</td></tr>
<tr><td>\f</td><td>Formfeed</td></tr>
<tr><td>\ + space</td><td>force a space character (useful at the end or start of a field)</td></tr>
</table>

An example of a (deliberately poorly formatted) table is -

```gherkin
| field1 | field2 |
|  hello  | fred   |
| goodbye       | bill    |
```

The data will be trimmed of spaces and hence the data will actually be as follows -

```clojure
'({"field1" "hello", "field2" "fred"} 
  {"field1" "goodbye", "field2" "bill"})
```

If you want to avoid the trimming, then you may escape the spaces with a backslash.

### Backgrounds
One or more *Background:* elements may be added to the file.  The syntax and structure 
follows that of a *Scenario*, but the background is run before each *Scenario:* or 
*Scenario Outline:* in the feature file.

In this example, the four steps in the background will be run before each of the 
scenarios in the feature file -

```gherkin
Feature: Multiple site support

  Background:
    Given a global administrator named "Greg"
    And a blog named "Greg's anti-tax rants"
    And a customer named "Wilson"
    And a blog named "Expensive Therapy" owned by "Wilson"

  Scenario: Wilson posts to his own blog
    Given I am logged in as Wilson
    When I try to post to "Expensive Therapy"
    Then I should see "Your article was published."

  Scenario: Greg posts to a client's blog
    Given I am logged in as Greg
    When I try to post to "Expensive Therapy"
    Then I should see "Your article was published."
```

This is semantically equivalent to - 

```gherkin
Feature: Multiple site support

  Scenario: Wilson posts to his own blog
    Given a global administrator named "Greg"
    And a blog named "Greg's anti-tax rants"
    And a customer named "Wilson"
    And a blog named "Expensive Therapy" owned by "Wilson"
    Given I am logged in as Wilson
    When I try to post to "Expensive Therapy"
    Then I should see "Your article was published."

  Scenario: Greg posts to a client's blog
    Given a global administrator named "Greg"
    And a blog named "Greg's anti-tax rants"
    And a customer named "Wilson"
    And a blog named "Expensive Therapy" owned by "Wilson"
    Given I am logged in as Greg
    When I try to post to "Expensive Therapy"
    Then I should see "Your article was published."
```

## Multiple line arguments

Multiple line arguments are supported in both *Scenario:* and *Background:* statements,
but not in a *Scenario Outline:*.  They come in two flavours - pystrings and tables.  In 
either case the step handler function will have an arity of 2 and the extra information
it passed in as a single piece of data.

### Pystrings
Pystrings are multiple line pre-formatted strings that conform to the Python Pystring
syntax - the text should be delimited by triple quotes (`"""`) on lines by themselves.
Strings may be indented and the spaces to the left of the indent will be stripped from
each line.  All other formatting is preserved.

```gherkin
Scenario: User entering free form text
  Given the user enters the following text into the web page
          """
          When the day has a blue sky
          then I feel much happier
          """
          ...
```

### Tables
Steps may also use the table format to describe data to be passed into the handler.

```gherkin
Scenario: Process a series of trades
  Given the following trades have been booked during the day
        | trade id | client | isin        | qty   | consideration |
        | 1        | CLI1   | UK010910291 | 1000  | GBP1000       |
        | 2        | CLI2   | US827387238 | 500   | USD2000       |
        ...
```

A list of maps representing the table will be passed into the step handler.  In this 
case the map would be - 

````clojure
    '({"trade id" "1", "client" "CLI1", "isin" "UK010910291", "qty" "1000", "consideration" "GBP1000"} 
      {"trade id" "2", "client" "CLI2", "isin" "US827387238", "qty" "500", "consideration" "USD2000"})
````


## License

Copyright © Nicholas Riordan.  All rights reserved.

Some small elements of code were taken from clojure.test and modified, and are
Copyright © Rich Hickey.  All rights reserved.

Distributed under the Eclipse Public License (as per Clojure).

