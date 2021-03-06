package com.redis

import org.scalatest.Spec
import org.scalatest.BeforeAndAfterEach
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith


@RunWith(classOf[JUnitRunner])
class SetOperationsSpec extends Spec 
                        with ShouldMatchers
                        with BeforeAndAfterEach
                        with BeforeAndAfterAll {

  val r = new RedisClient("localhost", 6379)

  override def beforeEach = {
  }

  override def afterEach = {
    r.flushdb
  }

  override def afterAll = {
    r.disconnect
  }

  describe("sadd") {
    it("should add a non-existent value to the set") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
    }
    it("should not add an existing value to the set") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "foo").get should equal(0)
    }
    it("should fail if the key points to a non-set") {
      r.lpush("list-1", "foo") should equal(true)
      val thrown = evaluating { r.sadd("list-1", "foo") } should produce [Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("srem") {
    it("should remove a value from the set") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.srem("set-1", "bar").get should equal(1)
      r.srem("set-1", "foo").get should equal(1)
    }
    it("should not do anything if the value does not exist") {
      r.sadd("set-1", "foo").get should equal(1)
      r.srem("set-1", "bar").get should equal(0)
    }
    it("should fail if the key points to a non-set") {
      r.lpush("list-1", "foo") should equal(true)
      val thrown = evaluating { r.srem("list-1", "foo") } should produce [Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("spop") {
    it("should pop a random element") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)
      r.spop("set-1").get should (equal("foo") or equal("bar") or equal("baz"))
    }
    it("should return nil if the key does not exist") {
      r.spop("set-1") should equal(None)
    }
  }

  describe("smove") {
    it("should move from one set to another") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)

      r.sadd("set-2", "1").get should equal(1)
      r.sadd("set-2", "2").get should equal(1)

      r.smove("set-1", "set-2", "baz").get should equal(1)
      r.sadd("set-2", "baz").get should equal(0)
      r.sadd("set-1", "baz").get should equal(1)
    }
    it("should return 0 if the element does not exist in source set") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)
      r.smove("set-1", "set-2", "bat").get should equal(0)
      r.smove("set-3", "set-2", "bat").get should equal(0)
    }
    it("should give error if the source or destination key is not a set") {
      r.lpush("list-1", "foo") should equal(true)
      r.lpush("list-1", "bar") should equal(true)
      r.lpush("list-1", "baz") should equal(true)
      r.sadd("set-1", "foo").get should equal(1)
      val thrown = evaluating { r.smove("list-1", "set-1", "bat") } should produce [Exception]
      thrown.getMessage should equal("ERR Operation against a key holding the wrong kind of value")
    }
  }

  describe("scard") {
    it("should return cardinality") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)
      r.scard("set-1").get should equal(3)
    }
    it("should return 0 if key does not exist") {
      r.scard("set-1").get should equal(0)
    }
  }

  describe("sismember") {
    it("should return true for membership") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)
      r.sismember("set-1", "foo") should equal(true)
    }
    it("should return false for no membership") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)
      r.sismember("set-1", "fo") should equal(false)
    }
    it("should return false if key does not exist") {
      r.sismember("set-1", "fo") should equal(false)
    }
  }

  describe("sinter") {
    it("should return intersection") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)

      r.sadd("set-2", "foo").get should equal(1)
      r.sadd("set-2", "bat").get should equal(1)
      r.sadd("set-2", "baz").get should equal(1)

      r.sadd("set-3", "for").get should equal(1)
      r.sadd("set-3", "bat").get should equal(1)
      r.sadd("set-3", "bay").get should equal(1)

      r.sinter("set-1", "set-2").get should equal(Set(Some("foo"), Some("baz")))
      r.sinter("set-1", "set-3").get should equal(Set.empty)
    }
    it("should return empty set for non-existing key") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)
      r.sinter("set-1", "set-4") should equal(None) // doc says it should be empty set
    }
  }

  describe("sinterstore") {
    it("should store intersection") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)

      r.sadd("set-2", "foo").get should equal(1)
      r.sadd("set-2", "bat").get should equal(1)
      r.sadd("set-2", "baz").get should equal(1)

      r.sadd("set-3", "for").get should equal(1)
      r.sadd("set-3", "bat").get should equal(1)
      r.sadd("set-3", "bay").get should equal(1)

      r.sinterstore("set-r", "set-1", "set-2").get should equal(2)
      r.scard("set-r").get should equal(2)
      r.sinterstore("set-s", "set-1", "set-3").get should equal(0)
      r.scard("set-s").get should equal(0)
    }
    it("should return empty set for non-existing key") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)
      r.sinterstore("set-r", "set-1", "set-4").get should equal(0)
      r.scard("set-r").get should equal(0)
    }
  }

  describe("sunion") {
    it("should return union") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)

      r.sadd("set-2", "foo").get should equal(1)
      r.sadd("set-2", "bat").get should equal(1)
      r.sadd("set-2", "baz").get should equal(1)

      r.sadd("set-3", "for").get should equal(1)
      r.sadd("set-3", "bat").get should equal(1)
      r.sadd("set-3", "bay").get should equal(1)

      r.sunion("set-1", "set-2").get should equal(Set(Some("foo"), Some("bar"), Some("baz"), Some("bat")))
      r.sunion("set-1", "set-3").get should equal(Set(Some("foo"), Some("bar"), Some("baz"), Some("for"), Some("bat"), Some("bay")))
    }
    it("should return empty set for non-existing key") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)
      r.sunion("set-1", "set-2").get should equal(Set(Some("foo"), Some("bar"), Some("baz")))
    }
  }

  describe("sunionstore") {
    it("should store union") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)

      r.sadd("set-2", "foo").get should equal(1)
      r.sadd("set-2", "bat").get should equal(1)
      r.sadd("set-2", "baz").get should equal(1)

      r.sadd("set-3", "for").get should equal(1)
      r.sadd("set-3", "bat").get should equal(1)
      r.sadd("set-3", "bay").get should equal(1)

      r.sunionstore("set-r", "set-1", "set-2").get should equal(4)
      r.scard("set-r").get should equal(4)
      r.sunionstore("set-s", "set-1", "set-3").get should equal(6)
      r.scard("set-s").get should equal(6)
    }
    it("should treat non-existing keys as empty sets") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)
      r.sunionstore("set-r", "set-1", "set-4").get should equal(3)
      r.scard("set-r").get should equal(3)
    }
  }

  describe("sdiff") {
    it("should return diff") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)

      r.sadd("set-2", "foo").get should equal(1)
      r.sadd("set-2", "bat").get should equal(1)
      r.sadd("set-2", "baz").get should equal(1)

      r.sadd("set-3", "for").get should equal(1)
      r.sadd("set-3", "bat").get should equal(1)
      r.sadd("set-3", "bay").get should equal(1)

      r.sdiff("set-1", "set-2", "set-3").get should equal(Set(Some("bar")))
    }
    it("should treat non-existing keys as empty sets") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)
      r.sdiff("set-1", "set-2").get should equal(Set(Some("foo"), Some("bar"), Some("baz")))
    }
  }

  describe("smembers") {
    it("should return members of a set") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)
      r.smembers("set-1").get should equal(Set(Some("foo"), Some("bar"), Some("baz")))
    }
    it("should return None for an empty set") {
      r.smembers("set-1") should equal(None)
    }
  }

  describe("srandmember") {
    it("should return a random member") {
      r.sadd("set-1", "foo").get should equal(1)
      r.sadd("set-1", "bar").get should equal(1)
      r.sadd("set-1", "baz").get should equal(1)
      r.srandmember("set-1").get should (equal("foo") or equal("bar") or equal("baz"))
    }
    it("should return None for a non-existing key") {
      r.srandmember("set-1") should equal(None)
    }
  }
}
