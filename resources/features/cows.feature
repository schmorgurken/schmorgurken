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

