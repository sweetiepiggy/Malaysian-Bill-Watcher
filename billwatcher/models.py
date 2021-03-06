import datetime

from sqlalchemy.ext.declarative import declarative_base
from sqlalchemy.orm import scoped_session, sessionmaker
from sqlalchemy.orm import relationship

from sqlalchemy import func
from sqlalchemy import Column
from sqlalchemy import Integer, String, ForeignKey, Date, DateTime
from sqlalchemy import create_engine
from sqlalchemy import desc

from settings import DB_CONNECT

maker = sessionmaker()

DBSession = scoped_session(maker)

Base = declarative_base()

def initdb():
    engine = create_engine(DB_CONNECT)
    Base.metadata.bind = engine
    Base.metadata.create_all(engine)

class Mixin(object):
    """Mixin to filter undefined keyword argument
    """
    def __init__(self, **kw):
        for k, v in kw.iteritems():
            if hasattr(self, k):
                setattr(self, k ,v)
    
class Bill(Mixin, Base):
    __tablename__ = 'bills'

    id = Column(Integer, autoincrement=True, primary_key=True)
    name = Column(String)
    long_name = Column(String)

    bill_revs = relationship('BillRevision', backref='bill',
                             order_by='desc(BillRevision.year)')

class BillRevision(Mixin, Base):
    __tablename__ = 'bill_revs'

    id = Column(Integer, autoincrement=True, primary_key=True)
    url = Column(String)
    status = Column(String)
    year = Column(Integer)
    read_by = Column(String)
    supported_by = Column(String)
    date_presented = Column(Date)
    bill_id = Column(Integer, ForeignKey('bills.id',
                                         onupdate='CASCADE',
                                         ondelete='CASCADE'))
    create_date = Column(DateTime, default=datetime.datetime.now)
    update_date = Column(DateTime)


class Language(Mixin,Base):
    __tablename__ = 'language'
    id = Column(Integer,autoincrement=True,primary_key=True)
    name = Column(String)

# I don't like to use name, but name is the essentially the key, 
# using this for now.
class BillTranslation(Mixin,Base):
    __tablename__ = 'bill_translation'
    id = Column(Integer,autoincrement=True,primary_key=True)
    lang_id = Column(Integer,ForeignKey('language.id',
                                        onupdate='CASCADE',
                                        ondelete='CASCADE'))
    name = Column(String)
    bill_id = Column(Integer,ForeignKey('bills.id',
                                            onupdate='CASCADE',
                                            ondelete='CASCADE'))

# year is not good enough for this, but then so is id
# so we going to use revs_id.bills.name?
class BillRevisionTranslation(Mixin,Base):
    __tablename__ = 'bill_rev_translation'
    id = Column(Integer,autoincrement=True,primary_key=True)
    lang_id = Column(Integer,ForeignKey('language.id',
                                        onupdate='CASCADE',
                                        ondelete='CASCADE'))
    name = Column(String)
    revs_id = Column(Integer,ForeignKey('bill_revs.id',
                                            onupdate='CASCADE',
                                            ondelete='CASCADE'))
