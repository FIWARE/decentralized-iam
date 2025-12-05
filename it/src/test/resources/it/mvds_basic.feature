Feature: The decentralized-iam component enables the authentication and authorization of services.

  Scenario: Operator from a registered organization can create energy reports from a context broker.
    Given organization is registered in the trusted issuer list.
    When a policy to allow users with OPERATOR role to create energy reports is registered.
    And organization issues a credential of type operator credential with OPERATOR role to its operator.
    And a valid access token with the operator credential is retrieved.
    Then employee can create a new energy report.

  Scenario: Employee from a registered organization can not create energy reports if he does not have the right role.
    Given organization is registered in the trusted issuer list.
    When a policy to allow users with OPERATOR role to create energy reports is registered.
    And organization issues a credential of type operator credential without OPERATOR role to its employee.
    And a valid access token with the operator credential is retrieved.
    Then employee should not be able to create a new energy report.
