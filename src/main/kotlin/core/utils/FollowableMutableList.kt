package core.utils

/**
 * Mutablelist wrapper with change listeners for reactive updates.
 */
class FollowableMutableList<T>(private val delegate: MutableList<T>) : MutableList<T> {
  private val followers = mutableListOf<(List<T>) -> Unit>()

  /**
   * Registers change listener that fires on list modifications.
   */
  fun follow(block: (List<T>) -> Unit) {
    followers.add(block)
  }

  private fun notifyFollowers() {
    followers.forEach { it(delegate) }
  }

  override val size: Int
    get() = delegate.size

  override fun contains(element: T): Boolean = delegate.contains(element)

  override fun containsAll(elements: Collection<T>): Boolean = delegate.containsAll(elements)

  override fun get(index: Int): T = delegate[index]

  override fun indexOf(element: T): Int = delegate.indexOf(element)

  override fun isEmpty(): Boolean = delegate.isEmpty()

  override fun iterator(): MutableIterator<T> = object : MutableIterator<T> {
    private val innerIterator = delegate.iterator()
    private var lastIndex: Int = -1

    override fun hasNext(): Boolean = innerIterator.hasNext()

    override fun next(): T {
      lastIndex++
      return innerIterator.next()
    }

    override fun remove() {
      innerIterator.remove()
      notifyFollowers()
    }
  }

  override fun lastIndexOf(element: T): Int = delegate.lastIndexOf(element)

  override fun add(element: T): Boolean {
    val result = delegate.add(element)
    if (result) notifyFollowers()
    return result
  }

  override fun add(index: Int, element: T) {
    delegate.add(index, element)
    notifyFollowers()
  }

  override fun addAll(index: Int, elements: Collection<T>): Boolean {
    val result = delegate.addAll(index, elements)
    if (result) notifyFollowers()
    return result
  }

  override fun addAll(elements: Collection<T>): Boolean {
    val result = delegate.addAll(elements)
    if (result) notifyFollowers()
    return result
  }

  override fun clear() {
    delegate.clear()
    notifyFollowers()
  }

  private inner class FollowableMutableListIterator(
    private val innerIterator: MutableListIterator<T>
  ) : MutableListIterator<T> {
    private var lastIndex: Int = -1

    override fun hasNext(): Boolean = innerIterator.hasNext()

    override fun next(): T {
      lastIndex = innerIterator.nextIndex()
      return innerIterator.next()
    }

    override fun hasPrevious(): Boolean = innerIterator.hasPrevious()

    override fun previous(): T {
      lastIndex = innerIterator.previousIndex()
      return innerIterator.previous()
    }

    override fun nextIndex(): Int = innerIterator.nextIndex()

    override fun previousIndex(): Int = innerIterator.previousIndex()

    override fun remove() {
      innerIterator.remove()
      notifyFollowers()
    }

    override fun set(element: T) {
      innerIterator.set(element)
      notifyFollowers()
    }

    override fun add(element: T) {
      innerIterator.add(element)
      notifyFollowers()
    }
  }

  override fun listIterator(): MutableListIterator<T> =
    FollowableMutableListIterator(delegate.listIterator())

  override fun listIterator(index: Int): MutableListIterator<T> =
    FollowableMutableListIterator(delegate.listIterator(index))

  override fun remove(element: T): Boolean {
    val result = delegate.remove(element)
    if (result) notifyFollowers()
    return result
  }

  override fun removeAll(elements: Collection<T>): Boolean {
    val result = delegate.removeAll(elements)
    if (result) notifyFollowers()
    return result
  }

  override fun retainAll(elements: Collection<T>): Boolean {
    val result = delegate.retainAll(elements)
    if (result) notifyFollowers()
    return result
  }

  override fun removeAt(index: Int): T {
    val result = delegate.removeAt(index)
    notifyFollowers()
    return result
  }

  override fun set(index: Int, element: T): T {
    val oldElement = delegate.set(index, element)
    notifyFollowers()
    return oldElement
  }

  override fun subList(fromIndex: Int, toIndex: Int): MutableList<T> =
    FollowableMutableList(delegate.subList(fromIndex, toIndex))
}