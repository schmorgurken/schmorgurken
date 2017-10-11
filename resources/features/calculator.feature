Feature: A calculator

  Scenario Outline: addition
    Given two numbers <a> and <b>
    When we calculate
    Then the result should be <result>
    Examples:
      | a   | b   | result |
      | 500 | 723 | 1223   |
      | 1   | 1   | 2      |
      | -1  | 1   | 0      |


  Scenario: multiplication
    When we multiply
      | a  | b | result |
      | 7  | 5 | 35     |
      | 15 | 2 | 30     |
