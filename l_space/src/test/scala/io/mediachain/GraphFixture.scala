package io.mediachain

import scala.util.Random


object GraphFixture {
  import io.mediachain.Types._
  import gremlin.scala._

  case class Objects(
     person: Person,
     personCanonical: Canonical,
     photoBlob: PhotoBlob,
     photoBlobCanonical: Canonical,
     modifiedPhotoBlob: PhotoBlob,
     extraPhotoBlob: PhotoBlob,
     extraPhotoBlobCanonical: Canonical
   )


  case class Context(graph: Graph, objects: Objects)
  object Context {
    def apply(graph: Graph): Context = {
      Context(graph, Util.setupTree(graph))
    }
  }

  object Util {
    // guarantees returned string is different from input
    // TODO: accept distance
    def mutate(s: String): String = {
      val idx = Random.nextInt(s.length)
      val chars = ('a' to 'z').toSet
      val replaced = s.charAt(idx)
      val replacing = (chars - replaced).toVector(Random.nextInt(chars.size - 1))
      s.updated(idx, replacing)
    }

    val stuffI = Random.shuffle(List("can of peas",
      "wishbone", "pair of glasses", "spool of wire", "wrench", "baseball hat", "television", "food",
      "wallet", "jar of pickles", "tea cup", "sketch pad", "towel", "game CD", "steak knife", "slipper",
      "pants", "sand paper", "boom box", "plush unicorn")).toIterator
    val foodI = Random.shuffle(List("Preserved Peaches", "Brussels Sprouts", "Bananas", "Lettuce Salad",
      "Olives", "Broiled Ham", "Cigars", "Mixed Green Salad", "Oyster Bay Asparagus", "Roast Lamb, Mint Sauce",
      "Lemonade", "Consomme en Tasse", "Liqueurs", "Iced Tea", "Canadian Club", "Radis", "Escarole Salad",
      "Preserved figs", "Potatoes, baked", "Macedoine salad")).toIterator
    def getPhotoBlob: PhotoBlob = {
      val title = stuffI.next
      val desc = foodI.next
      // FIXME: randomize date
      val date = "2016-02-22T19:04:13+00:00"
      PhotoBlob(None, title, desc, date, None)
    }

    def getModifiedPhotoBlob: PhotoBlob = {
      val b = getPhotoBlob
      b.copy(description = mutate(b.description))
    }

    val bodhisattvasI = Random.shuffle(List("Avalokitesvara",
      "Manjushri", "Samantabhadra", "Kshitigarbha", "Maitreya", "Mahasthamaprapta", "Ākāśagarbha")).toIterator
    def getPerson: Person = Person.create(bodhisattvasI.next)

    def setupTree(graph: Graph): Objects = {
      // add photo and canonical
      val photoBlob = getPhotoBlob
      val photoBlobV = graph + photoBlob
      val photoBlobCanonical = Canonical.create
      val canonicalV = graph + photoBlobCanonical
      canonicalV --- DescribedBy --> photoBlobV

      // add a revision to a photo
      val modifiedBlob = getModifiedPhotoBlob
      val modifiedBlobV = graph + modifiedBlob
      photoBlobV --- ModifiedBy --> modifiedBlobV

      // add an author for the photo
      val person = getPerson
      val personV = graph + person
      val personCanonical = Canonical.create()
      val personCanonicalV = graph + personCanonical
      personCanonicalV --- DescribedBy --> personV
      photoBlobV --- AuthoredBy --> personCanonicalV

      // add decoy objects that we shouldn't see in a subtree
      val extraPhotoBlob = getPhotoBlob
      val extraPhotoBlobV = graph + extraPhotoBlob
      val extraPhotoBlobCanonical = Canonical.create()
      val extraPhotoBlobCanonicalV = graph + extraPhotoBlobCanonical
      extraPhotoBlobCanonicalV --- DescribedBy --> extraPhotoBlobV
      extraPhotoBlobV --- AuthoredBy --> personCanonicalV

      Objects(
        personV.toCC[Person],
        personCanonicalV.toCC[Canonical],
        photoBlobV.toCC[PhotoBlob],
        canonicalV.toCC[Canonical],
        modifiedBlobV.toCC[PhotoBlob],
        extraPhotoBlobV.toCC[PhotoBlob],
        extraPhotoBlobCanonicalV.toCC[Canonical])
    }
  }

}